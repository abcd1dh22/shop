package com.hawatraders.manager.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawatraders.manager.data.db.ProductEntity
import com.hawatraders.manager.data.repository.ProductRepository
import com.hawatraders.manager.util.Strings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ProductManageViewModel(private val repo: ProductRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    /** null until the first database result arrives. */
    val products: StateFlow<List<ProductEntity>?> = _query
        .debounce { if (it.isBlank()) 0L else 250L }
        .flatMapLatest { repo.observeSearch(it, null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var message by mutableStateOf<String?>(null)

    fun onQueryChange(v: String) { _query.value = v }

    fun delete(product: ProductEntity) {
        viewModelScope.launch {
            try {
                repo.delete(product)
                message = Strings.deleted
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = Strings.deleteFailed
            }
        }
    }

    fun updatePrice(product: ProductEntity, price: Double) {
        viewModelScope.launch {
            try {
                repo.updatePrice(product.id, price)
                message = Strings.priceUpdated
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = Strings.saveFailed
            }
        }
    }

    fun toggleFavorite(product: ProductEntity) {
        viewModelScope.launch {
            try {
                repo.setFavorite(product.id, !product.isFavorite)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = Strings.saveFailed
            }
        }
    }

    fun consumeMessage() { message = null }
}
