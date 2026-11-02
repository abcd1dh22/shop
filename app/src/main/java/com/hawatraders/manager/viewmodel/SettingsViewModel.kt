package com.hawatraders.manager.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawatraders.manager.data.OwnerSession
import com.hawatraders.manager.data.repository.SettingsRepository
import com.hawatraders.manager.data.repository.VerifyResult
import com.hawatraders.manager.util.Strings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the Settings gate: first-run password creation and owner login with attempt lockout.
 * Typed passwords live only in memory here (survive rotation, never saved to disk or state bundles).
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val session: OwnerSession,
) : ViewModel() {

    /** null while the first DataStore read is in flight. */
    val hasPassword: StateFlow<Boolean?> = settings.hasPassword
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val unlocked: StateFlow<Boolean> = session.unlocked

    var password by mutableStateOf("")
    var confirm by mutableStateOf("")
    var passwordError by mutableStateOf<String?>(null)
    var confirmError by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    var lockSeconds by mutableIntStateOf(0)
        private set

    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            val left = settings.remainingLockSeconds()
            if (left > 0) startLockCountdown(left)
        }
    }

    fun onPasswordChange(value: String) {
        password = value
        passwordError = null
    }

    fun onConfirmChange(value: String) {
        confirm = value
        confirmError = null
    }

    fun createPassword() {
        if (busy) return
        passwordError = if (password.length < MIN_LENGTH) Strings.passwordTooShort(MIN_LENGTH) else null
        confirmError = if (password != confirm) Strings.passwordMismatch else null
        if (passwordError != null || confirmError != null) return

        busy = true
        viewModelScope.launch {
            val chars = password.toCharArray()
            try {
                settings.createPassword(chars)
                password = ""
                confirm = ""
                session.unlock()
            } catch (e: Exception) {
                passwordError = Strings.genericError
            } finally {
                chars.fill('\u0000')
                busy = false
            }
        }
    }

    fun login() {
        if (busy || lockSeconds > 0 || password.isEmpty()) return
        busy = true
        viewModelScope.launch {
            val chars = password.toCharArray()
            try {
                when (val r = settings.verifyPassword(chars)) {
                    VerifyResult.Success -> {
                        password = ""
                        passwordError = null
                        session.unlock()
                    }
                    is VerifyResult.Wrong -> passwordError = Strings.wrongPassword(r.attemptsLeft)
                    is VerifyResult.Locked -> {
                        passwordError = null
                        startLockCountdown(r.secondsLeft)
                    }
                    VerifyResult.NotSet -> passwordError = Strings.genericError
                }
            } catch (e: Exception) {
                passwordError = Strings.genericError
            } finally {
                chars.fill('\u0000')
                busy = false
            }
        }
    }

    fun lock() = session.lock()

    private fun startLockCountdown(seconds: Long) {
        ticker?.cancel()
        lockSeconds = seconds.toInt()
        ticker = viewModelScope.launch {
            while (lockSeconds > 0) {
                delay(1_000)
                lockSeconds -= 1
            }
        }
    }

    companion object {
        const val MIN_LENGTH = 4
    }
}
