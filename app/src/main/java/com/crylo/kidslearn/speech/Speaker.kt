package com.crylo.kidslearn.speech

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class Language { ENGLISH, HINDI }

/**
 * Thin wrapper over the platform [TextToSpeech]: no bundled audio, no network of our own.
 * Whatever engine the phone has (usually Google's, which works offline once a voice is
 * downloaded) does the speaking. [state] and [hindiReady] are Compose state so the UI
 * can show the "install a voice" note as soon as we know.
 */
class Speaker(context: Context, private val rate: () -> Float) {
    enum class State { STARTING, READY, NO_ENGINE }

    var state by mutableStateOf(State.STARTING)
        private set

    /** Optimistic until checked, so the note does not flash on every launch. */
    var hindiReady by mutableStateOf(true)
        private set

    private var englishLocale: Locale = Locale.ENGLISH
    private var utterance = 0
    private val tts = TextToSpeech(context.applicationContext) { onInit(it) }

    private fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            state = State.NO_ENGINE
            return
        }
        // Indian English first: it says "zed" and the names the way Indian parents do.
        englishLocale = ENGLISH_PREFERENCE.firstOrNull { availability(it) >= TextToSpeech.LANG_AVAILABLE }
            ?: Locale.ENGLISH
        state = State.READY
        refresh()
    }

    /** Re-checks the Hindi voice; called on resume, as a parent may have just installed it. */
    fun refresh() {
        if (state == State.READY) hindiReady = hasLocalVoice(HINDI)
    }

    fun speak(text: String, language: Language, queue: Boolean = false) {
        if (state != State.READY) return
        val result = tts.setLanguage(if (language == Language.HINDI) HINDI else englishLocale)
        // Without the voice the engine would read Devanagari with an English voice, which
        // is worse than silence.
        if (result < TextToSpeech.LANG_AVAILABLE) return
        tts.setSpeechRate(rate())
        val mode = if (queue) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        tts.speak(text, mode, null, "u${utterance++}")
    }

    fun stop() {
        if (state == State.READY) tts.stop()
    }

    fun shutdown() = tts.shutdown()

    /**
     * Opens the engine's "install voice data" screen, falling back to the system
     * text-to-speech settings. Only ever reached through the parent gate.
     */
    fun openVoiceInstall(context: Context) {
        val install = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            runCatching { tts.defaultEngine }.getOrNull()?.let { setPackage(it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val settings = Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        for (intent in listOf(install, settings)) {
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next one.
            } catch (_: SecurityException) {
                // Some engines do not export the install activity.
            }
        }
    }

    private fun availability(locale: Locale): Int =
        runCatching { tts.isLanguageAvailable(locale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)

    /**
     * "Available" from the engine can mean "downloadable" or "network only". A voice
     * counts only if it is installed and works offline; engines that list no voices at
     * all are trusted on the language check alone.
     */
    private fun hasLocalVoice(locale: Locale): Boolean {
        if (availability(locale) < TextToSpeech.LANG_AVAILABLE) return false
        val voices = runCatching { tts.voices }.getOrNull().orEmpty()
            .filter { it.locale.language == locale.language }
        if (voices.isEmpty()) return true
        return voices.any { voice ->
            !voice.isNetworkConnectionRequired &&
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty()
        }
    }

    companion object {
        val HINDI: Locale = Locale.forLanguageTag("hi-IN")
        private val ENGLISH_PREFERENCE = listOf("en-IN", "en-GB", "en-US").map(Locale::forLanguageTag)
        const val RATE_SLOW = 0.75f
        const val RATE_NORMAL = 1.0f
    }
}
