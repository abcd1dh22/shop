package com.hawatraders.manager.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawatraders.manager.data.repository.SettingsRepository
import com.hawatraders.manager.data.repository.VerifyResult
import com.hawatraders.manager.util.Strings
import kotlinx.coroutines.launch

class ChangePasswordViewModel(private val settings: SettingsRepository) : ViewModel() {
    var current by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var confirm by mutableStateOf("")
    var currentError by mutableStateOf<String?>(null)
    var newError by mutableStateOf<String?>(null)
    var confirmError by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    var done by mutableStateOf(false)

    fun onCurrent(v: String) { current = v; currentError = null }
    fun onNew(v: String) { newPassword = v; newError = null }
    fun onConfirm(v: String) { confirm = v; confirmError = null }

    fun submit() {
        if (busy) return
        currentError = if (current.isEmpty()) Strings.passwordTooShort(1) else null
        newError = if (newPassword.length < SettingsViewModel.MIN_LENGTH)
            Strings.passwordTooShort(SettingsViewModel.MIN_LENGTH) else null
        confirmError = if (newPassword != confirm) Strings.passwordMismatch else null
        if (currentError != null || newError != null || confirmError != null) return

        busy = true
        viewModelScope.launch {
            val cur = current.toCharArray()
            val new = newPassword.toCharArray()
            try {
                when (val r = settings.changePassword(cur, new)) {
                    VerifyResult.Success -> {
                        current = ""; newPassword = ""; confirm = ""
                        done = true
                    }
                    is VerifyResult.Wrong -> currentError = Strings.wrongPassword(r.attemptsLeft)
                    is VerifyResult.Locked -> currentError = Strings.lockedFor(r.secondsLeft)
                    VerifyResult.NotSet -> currentError = Strings.genericError
                }
            } catch (e: Exception) {
                currentError = Strings.genericError
            } finally {
                cur.fill('\u0000')
                new.fill('\u0000')
                busy = false
            }
        }
    }
}
