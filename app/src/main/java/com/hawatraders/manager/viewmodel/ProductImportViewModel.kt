package com.hawatraders.manager.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawatraders.manager.data.importer.ParsedImport
import com.hawatraders.manager.data.importer.ProductImporter
import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.Strings
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface ProductImportState {
    data object Idle : ProductImportState
    data class Working(val label: String) : ProductImportState
    data class Preview(val parsed: ParsedImport) : ProductImportState
}

/** Pick file -> parse and preview (nothing written) -> confirm -> apply in one transaction. */
class ProductImportViewModel(private val importer: ProductImporter) : ViewModel() {
    var state by mutableStateOf<ProductImportState>(ProductImportState.Idle)
        private set
    var message by mutableStateOf<String?>(null)

    /** A CSV the screen should hand to the share sheet; the screen clears it afterwards. */
    var shareRequest by mutableStateOf<File?>(null)

    fun onFilePicked(uri: Uri) {
        state = ProductImportState.Working(Strings.importReading)
        viewModelScope.launch {
            try {
                state = ProductImportState.Preview(importer.parse(uri))
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                state = ProductImportState.Idle
                message = e.userMessage
            } catch (e: Exception) {
                state = ProductImportState.Idle
                message = Strings.errReadFile
            }
        }
    }

    fun confirm() {
        val parsed = (state as? ProductImportState.Preview)?.parsed ?: return
        if (parsed.rows.isEmpty()) {
            state = ProductImportState.Idle
            return
        }
        state = ProductImportState.Working(Strings.importApplying)
        viewModelScope.launch {
            try {
                importer.apply(parsed)
                message = Strings.importApplied(parsed.rows.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                message = e.userMessage
            } catch (e: Exception) {
                message = Strings.saveFailed
            } finally {
                state = ProductImportState.Idle
            }
        }
    }

    fun cancel() {
        state = ProductImportState.Idle
    }

    fun makeTemplate() = share { importer.writeTemplate() }

    fun makeExport() = share { importer.writeExport() }

    private fun share(build: suspend () -> File) {
        viewModelScope.launch {
            try {
                shareRequest = build()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = Strings.genericError
            }
        }
    }

    fun consumeMessage() {
        message = null
    }
}
