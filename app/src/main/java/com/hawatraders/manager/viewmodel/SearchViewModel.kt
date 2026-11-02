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
)

/** Search text, category filter and the selected product survive rotation and process death. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    repo: ProductRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {

    private val _query = MutableStateFlow(handle.get<String>(KEY_QUERY) ?: "")
    val query: StateFlow<String> = _query

    private val _category = MutableStateFlow(Category.fromKeyOrNull(handle.get<String>(KEY_CATEGORY)))
    val category: StateFlow<Category?> = _category

    private val _selectedId = MutableStateFlow(handle.get<Long>(KEY_SELECTED))
    val selectedId: StateFlow<Long?> = _selectedId

    val uiState: StateFlow<SearchUiState> =
        combine(
            // 250ms debounce for typing, but clearing the box reacts immediately.
            _query.debounce { if (it.isBlank()) 0L else 250L },
            _category,
        ) { q, c -> q to c }
            .flatMapLatest { (q, c) -> repo.observeSearch(q, c) }
            .combine(repo.observeCount()) { list, count -> SearchUiState(list, count, isLoading = false) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        _query.value = value
        handle[KEY_QUERY] = value
    }

    fun onCategoryChange(category: Category?) {
        _category.value = category
        handle[KEY_CATEGORY] = category?.key
    }

    fun select(id: Long?) {
        _selectedId.value = id
        handle[KEY_SELECTED] = id
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_CATEGORY = "category"
        const val KEY_SELECTED = "selected"
    }
}
