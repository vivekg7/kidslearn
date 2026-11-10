package com.crylo.kidslearn

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * The two parent settings, in SharedPreferences. Two booleans do not justify DataStore's
 * coroutine plumbing; reads are instant and writes are asynchronous with `apply`.
 */
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Slow by default: small children follow 0.75× speech more easily. */
    var slowSpeech by mutableStateOf(prefs.getBoolean(KEY_SLOW, true))
        private set

    var autoSpeak by mutableStateOf(prefs.getBoolean(KEY_AUTO, true))
        private set

    fun updateSlowSpeech(value: Boolean) {
        slowSpeech = value
        prefs.edit { putBoolean(KEY_SLOW, value) }
    }

    fun updateAutoSpeak(value: Boolean) {
        autoSpeak = value
        prefs.edit { putBoolean(KEY_AUTO, value) }
    }

    private companion object {
        const val KEY_SLOW = "slow_speech"
        const val KEY_AUTO = "auto_speak"
    }
}
