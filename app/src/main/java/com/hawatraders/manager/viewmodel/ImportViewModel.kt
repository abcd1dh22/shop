package com.hawatraders.manager.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawatraders.manager.data.backup.BackupManager
import com.hawatraders.manager.data.backup.BackupPayload
import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.Strings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface ImportState {
    data object Idle : ImportState
    data class NeedPassword(val uri: Uri, val error: String? = null) : ImportState
    data class Working(val label: String) : ImportState
    data class Confirm(val payload: BackupPayload) : ImportState
}

/**
 * Import flow: pick file -> backup password -> decrypt/validate (nothing changed yet) ->
 * confirmation -> atomic restore. Available without the owner password.
 */
class ImportViewModel(private val manager: BackupManager) : ViewModel() {
    var state by mutableStateOf<ImportState>(ImportState.Idle)
        private set
    var password by mutableStateOf("")
    var message by mutableStateOf<String?>(null)

    fun onFilePicked(uri: Uri) {
        password = ""
        state = ImportState.NeedPassword(uri)
    }

    fun cancel() {
        password = ""
        state = ImportState.Idle
    }

    fun submitPassword() {
        val current = state as? ImportState.NeedPassword ?: return
        if (password.isEmpty()) return
        state = ImportState.Working(Strings.importVerifying)
        viewModelScope.launch {
            val chars = password.toCharArray()
            try {
                val payload = manager.readBackup(current.uri, chars)
                password = ""
                state = ImportState.Confirm(payload)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                if (e.userMessage == Strings.errWrongBackupPassword) {
                    // Keep the dialog open so the owner can retype.
                    state = ImportState.NeedPassword(current.uri, e.userMessage)
                } else {
                    state = ImportState.Idle
                    message = e.userMessage
                }
            } catch (e: Exception) {
                state = ImportState.Idle
                message = Strings.errBackupCorrupt
            } finally {
                chars.fill('\u0000')
            }
        }
    }

    fun confirmRestore() {
        val confirm = state as? ImportState.Confirm ?: return
        state = ImportState.Working(Strings.importRestoring)
        viewModelScope.launch {
            try {
                manager.restore(confirm.payload)
                message = Strings.importDone(confirm.payload.products.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                message = e.userMessage
            } catch (e: Exception) {
                message = Strings.errRestore
            } finally {
                state = ImportState.Idle
            }
        }
    }

    fun consumeMessage() { message = null }
}
