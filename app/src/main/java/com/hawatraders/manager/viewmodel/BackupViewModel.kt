package com.hawatraders.manager.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawatraders.manager.data.backup.BackupManager
import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.Strings
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class BackupViewModel(private val manager: BackupManager) : ViewModel() {
    var password by mutableStateOf("")
    var confirm by mutableStateOf("")
    var passwordError by mutableStateOf<String?>(null)
    var confirmError by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)

    /** The encrypted file, once created; ready to save via SAF or share. */
    var readyFile by mutableStateOf<File?>(null)
        private set
    var message by mutableStateOf<String?>(null)

    fun onPassword(v: String) { password = v; passwordError = null; readyFile = null }
    fun onConfirm(v: String) { confirm = v; confirmError = null }

    fun create() {
        if (busy) return
        passwordError = if (password.length < MIN_LENGTH) Strings.backupPasswordShort(MIN_LENGTH) else null
        confirmError = if (password != confirm) Strings.passwordMismatch else null
        if (passwordError != null || confirmError != null) return

        busy = true
        readyFile = null
        viewModelScope.launch {
            val chars = password.toCharArray()
            try {
                readyFile = manager.createBackupFile(chars)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                message = e.userMessage
            } catch (e: Exception) {
                message = Strings.backupFailed
            } finally {
                chars.fill('\u0000')
                busy = false
            }
        }
    }

    fun saveTo(uri: Uri) {
        val file = readyFile ?: return
        busy = true
        viewModelScope.launch {
            try {
                manager.copyTo(file, uri)
                message = Strings.backupSaved
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                message = e.userMessage
            } catch (e: Exception) {
                message = Strings.fileSaveFailed
            } finally {
                busy = false
            }
        }
    }

    fun consumeMessage() { message = null }

    override fun onCleared() {
        manager.deleteTempBackups()
    }

    companion object {
        const val MIN_LENGTH = 6
    }
}
