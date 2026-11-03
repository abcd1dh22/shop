package com.hawatraders.manager.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawatraders.manager.data.db.Category
import com.hawatraders.manager.data.db.ProductEntity
import com.hawatraders.manager.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val products: List<ProductEntity> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val counts: Map<Category, Int> = emptyMap(),
    val favoriteCount: Int = 0,
)

/** Search text, filters and the selected product survive rotation and process death. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    repo: ProductRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {

    private val _query = MutableStateFlow(handle.get<String>(KEY_QUERY) ?: "")
    val query: StateFlow<String> = _query

    private val _category = MutableStateFlow(Category.fromKeyOrNull(handle.get<String>(KEY_CATEGORY)))
    val category: StateFlow<Category?> = _category

    private val _favoritesOnly = MutableStateFlow(handle.get<Boolean>(KEY_FAVORITES) ?: false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly

    private val _selectedId = MutableStateFlow(handle.get<Long>(KEY_SELECTED))
    val selectedId: StateFlow<Long?> = _selectedId

    val uiState: StateFlow<SearchUiState> =
        combine(
            // 250ms debounce for typing, but clearing the box reacts immediately.
            _query.debounce { if (it.isBlank()) 0L else 250L },
            _category,
            _favoritesOnly,
        ) { q, c, f -> Triple(q, c, f) }
            .flatMapLatest { (q, c, f) -> repo.observeSearch(q, c, f) }
            .combine(repo.observeAll()) { list, all ->
                SearchUiState(
                    products = list,
                    totalCount = all.size,
                    isLoading = false,
                    counts = all.groupingBy { it.category }.eachCount(),
                    favoriteCount = all.count { it.isFavorite },
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        _query.value = value
        handle[KEY_QUERY] = value
    }

    fun onCategoryChange(category: Category?) {
        _category.value = category
        handle[KEY_CATEGORY] = category?.key
    }

    fun setFavoritesOnly(value: Boolean) {
        _favoritesOnly.value = value
        handle[KEY_FAVORITES] = value
    }

    fun select(id: Long?) {
        _selectedId.value = id
        handle[KEY_SELECTED] = id
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_CATEGORY = "category"
        const val KEY_FAVORITES = "favorites"
        const val KEY_SELECTED = "selected"
    }
}
