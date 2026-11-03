package com.hawatraders.manager.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawatraders.manager.data.ocr.TextScanner
import com.hawatraders.manager.data.repository.ProductRepository
import com.hawatraders.manager.data.db.ProductEntity
import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.ProductMatcher
import com.hawatraders.manager.util.Strings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface TextScanState {
    data object Idle : TextScanState
    data object Working : TextScanState
    data class Result(val matches: List<ProductEntity>, val text: String) : TextScanState
    data class Failed(val message: String) : TextScanState
}

/** Photo of a product label -> OCR -> ranked list of matching products. */
class TextScanViewModel(
    private val scanner: TextScanner,
    private val repo: ProductRepository,
) : ViewModel() {

    var state by mutableStateOf<TextScanState>(TextScanState.Idle)
        private set

    fun process(photo: Uri) {
        if (state == TextScanState.Working) return
        state = TextScanState.Working
        viewModelScope.launch {
            state = try {
                val text = scanner.recognize(photo)
                val matches = ProductMatcher.rank(text, repo.getAll())
                TextScanState.Result(matches, text.trim())
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                TextScanState.Failed(e.userMessage)
            } catch (e: Exception) {
                TextScanState.Failed(Strings.ocrEngineFailed)
            }
        }
    }

    fun dismiss() {
        state = TextScanState.Idle
    }
}
