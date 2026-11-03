package com.hawatraders.manager.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawatraders.manager.data.db.ProductEntity
import com.hawatraders.manager.data.repository.ProductRepository
import com.hawatraders.manager.util.Bn
import com.hawatraders.manager.util.PriceMath
import com.hawatraders.manager.util.Strings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Raise or lower prices for all products, one category and/or one company, by % or a fixed taka amount. */
class BulkPriceViewModel(private val repo: ProductRepository) : ViewModel() {
    var categoryKey by mutableStateOf<String?>(null)
    var company by mutableStateOf("")
    var increase by mutableStateOf(true)
    var byPercent by mutableStateOf(true)
    var amountText by mutableStateOf("")
    var busy by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    var all by mutableStateOf<List<ProductEntity>>(emptyList())
        private set

    init {
        viewModelScope.launch { repo.observeAll().collect { all = it } }
    }

    /** Parsed amount, or null when empty / invalid / not above zero. */
    val amount: Double? get() = Bn.parsePrice(amountText)?.takeIf { it > 0.0 }

    /** Products matched by the current category + company filter. */
    fun affected(): List<ProductEntity> {
        val q = company.trim()
        return all.filter { p ->
            (categoryKey == null || p.category.key == categoryKey) &&
                (q.isEmpty() || p.company.contains(q, ignoreCase = true))
        }
    }

    fun newPrice(p: ProductEntity): Double? =
        amount?.let { PriceMath.adjust(p.price, byPercent, increase, it) }

    fun apply() {
        val a = amount ?: return
        val targets = affected()
        if (targets.isEmpty() || busy) return
        busy = true
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val changed = targets.mapNotNull { p ->
                    val np = PriceMath.adjust(p.price, byPercent, increase, a)
                    if (np == p.price) null else p.copy(price = np, updatedAt = now, priceUpdatedAt = now)
                }
                repo.applyPrices(changed)
                message = Strings.bulkDone(changed.size)
                amountText = ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = Strings.saveFailed
            } finally {
                busy = false
            }
        }
    }
}
