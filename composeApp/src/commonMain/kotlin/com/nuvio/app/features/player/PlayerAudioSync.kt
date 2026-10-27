package com.nuvio.app.features.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sound sync (audio delay), for Bluetooth headphones and speakers that lag behind the picture.
 * Only players that can shift audio turn it on (the desktop app); elsewhere the control stays hidden.
 */
object PlayerAudioSync {
    const val MIN_MS = -5_000
    const val MAX_MS = 5_000
    const val STEP_MS = 50

    var supported by mutableStateOf(false)
        private set

    private val _delayMs = MutableStateFlow(0)
    val delayMs: StateFlow<Int> = _delayMs

    private var persist: (Int) -> Unit = {}

    fun enable(savedDelayMs: Int, persist: (Int) -> Unit) {
        if (supported) return
        _delayMs.value = savedDelayMs.coerceIn(MIN_MS, MAX_MS)
        this.persist = persist
        supported = true
    }

    fun setDelayMs(delayMs: Int) {
        val clamped = delayMs.coerceIn(MIN_MS, MAX_MS)
        _delayMs.value = clamped
        persist(clamped)
    }
}
