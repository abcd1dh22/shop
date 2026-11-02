package com.hawatraders.manager.data.repository

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hawatraders.manager.data.crypto.PasswordHasher
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

sealed interface VerifyResult {
    data object Success : VerifyResult
    data object NotSet : VerifyResult
    data class Wrong(val attemptsLeft: Int) : VerifyResult
    data class Locked(val secondsLeft: Long) : VerifyResult
}

/** Stores the owner password as PBKDF2 hash + salt and tracks failed attempts persistently. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore
    private val mutex = Mutex()

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val hasPassword: Flow<Boolean> =
        data.map { it[HASH] != null && it[SALT] != null }.distinctUntilChanged()

    suspend fun createPassword(password: CharArray) {
        storeNewPassword(password)
    }

    suspend fun verifyPassword(password: CharArray): VerifyResult = mutex.withLock {
        val prefs = data.first()
        val hashB64 = prefs[HASH]
        val saltB64 = prefs[SALT]
        if (hashB64 == null || saltB64 == null) return@withLock VerifyResult.NotSet

        val now = System.currentTimeMillis()
        val lockUntil = prefs[LOCK_UNTIL] ?: 0L
        if (lockUntil > now) return@withLock VerifyResult.Locked(secondsCeil(lockUntil - now))

        val iterations = prefs[ITER] ?: PasswordHasher.ITERATIONS
        val ok = withContext(Dispatchers.Default) {
            PasswordHasher.verify(
                password, Base64.decode(saltB64, Base64.NO_WRAP),
                Base64.decode(hashB64, Base64.NO_WRAP), iterations,
            )
        }
        if (ok) {
            store.edit {
                it[FAILS] = 0
                it[LOCK_UNTIL] = 0L
            }
            return@withLock VerifyResult.Success
        }

        val fails = (prefs[FAILS] ?: 0) + 1
        if (fails % MAX_ATTEMPTS == 0) {
            // 30s, 60s, 120s, 240s, then capped at 5 minutes for every further round of 5 failures.
            val round = fails / MAX_ATTEMPTS
            val delaySec = (30L * (1L shl (round - 1).coerceAtMost(4))).coerceAtMost(300L)
            store.edit {
                it[FAILS] = fails
                it[LOCK_UNTIL] = now + delaySec * 1000
            }
            VerifyResult.Locked(delaySec)
        } else {
            store.edit { it[FAILS] = fails }
            VerifyResult.Wrong(MAX_ATTEMPTS - fails % MAX_ATTEMPTS)
        }
    }

    /** Verifies [current] (counting toward lockout) and, if correct, stores [new]. */
    suspend fun changePassword(current: CharArray, new: CharArray): VerifyResult {
        val r = verifyPassword(current)
        if (r is VerifyResult.Success) storeNewPassword(new)
        return r
    }

    /** Seconds left of an active lockout, or 0. */
    suspend fun remainingLockSeconds(): Long {
        val until = data.first()[LOCK_UNTIL] ?: 0L
        val now = System.currentTimeMillis()
        return if (until > now) secondsCeil(until - now) else 0L
    }

    private suspend fun storeNewPassword(password: CharArray) {
        val salt = PasswordHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(password, salt) }
        store.edit {
            it[HASH] = Base64.encodeToString(hash, Base64.NO_WRAP)
            it[SALT] = Base64.encodeToString(salt, Base64.NO_WRAP)
            it[ITER] = PasswordHasher.ITERATIONS
            it[FAILS] = 0
            it[LOCK_UNTIL] = 0L
        }
    }

    private fun secondsCeil(ms: Long): Long = (ms + 999) / 1000

    private companion object {
        const val MAX_ATTEMPTS = 5
        val HASH = stringPreferencesKey("pw_hash")
        val SALT = stringPreferencesKey("pw_salt")
        val ITER = intPreferencesKey("pw_iterations")
        val FAILS = intPreferencesKey("failed_attempts")
        val LOCK_UNTIL = longPreferencesKey("lock_until")
    }
}
