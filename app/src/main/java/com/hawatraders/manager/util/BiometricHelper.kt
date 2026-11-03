package com.hawatraders.manager.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Fingerprint (Class 3 / "strong" biometric) check used as a convenient alternative to typing the
 * owner password. The password always remains available through the dialog's negative button.
 */
object BiometricHelper {
    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_STRONG

    /** True when the phone has biometric hardware and at least one enrolled fingerprint/face. */
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun isLockout(errorCode: Int): Boolean =
        errorCode == BiometricPrompt.ERROR_LOCKOUT || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT

    /** [onError] also fires when the user cancels or taps "পাসওয়ার্ড দিন"; callers simply ignore those. */
    fun prompt(activity: FragmentActivity, onSuccess: () -> Unit, onError: (Int) -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errorCode)
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(Strings.bioTitle)
            .setSubtitle(Strings.bioSubtitle)
            .setNegativeButtonText(Strings.bioUsePassword)
            .setAllowedAuthenticators(AUTH)
            .setConfirmationRequired(false)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
    }
}
