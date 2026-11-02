package com.hawatraders.manager.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory "owner unlocked" state. Deliberately never persisted: a killed process starts locked.
 */
class OwnerSession {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    /**
     * Set to true right before launching another activity (camera, picker, share sheet) so that the
     * resulting onStop does not auto-lock the owner area while they are mid-edit.
     */
    @Volatile
    var externalLaunch: Boolean = false

    fun unlock() {
        _unlocked.value = true
    }

    fun lock() {
        _unlocked.value = false
    }
}
