package com.ssafy.e102.eumgil.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface TextToSpeechController {
    val state: StateFlow<TextToSpeechState>

    fun setEnabled(enabled: Boolean)

    fun speak(text: String)

    fun stop()

    fun shutdown()
}

data class TextToSpeechState(
    val enabled: Boolean = true,
    val availability: TextToSpeechAvailability = TextToSpeechAvailability.Initializing,
) {
    val canSpeak: Boolean
        get() = enabled && availability == TextToSpeechAvailability.Ready
}

enum class TextToSpeechAvailability {
    Initializing,
    Ready,
    Unavailable,
}

class AndroidTextToSpeechController(
    context: Context,
    private val locale: Locale = Locale.KOREAN,
) : TextToSpeechController {
    private val appContext = context.applicationContext
    private var engine: TextToSpeech? = null
    private var pendingText: String? = null
    private var isShutdown = false

    private val mutableState = MutableStateFlow(TextToSpeechState())
    override val state: StateFlow<TextToSpeechState> = mutableState.asStateFlow()

    override fun setEnabled(enabled: Boolean) {
        mutableState.update { it.copy(enabled = enabled) }
        if (!enabled) {
            stop()
        } else {
            ensureEngine()
        }
    }

    override fun speak(text: String) {
        val utterance = text.trim()
        if (utterance.isEmpty() || isShutdown || !state.value.enabled) return

        pendingText = utterance
        ensureEngine()
        if (state.value.canSpeak) {
            speakPendingText()
        }
    }

    override fun stop() {
        pendingText = null
        runCatching { engine?.stop() }
    }

    override fun shutdown() {
        isShutdown = true
        pendingText = null
        runCatching { engine?.stop() }
        runCatching { engine?.shutdown() }
        engine = null
        mutableState.update { it.copy(availability = TextToSpeechAvailability.Unavailable) }
    }

    private fun ensureEngine() {
        if (engine != null || isShutdown) return

        mutableState.update { it.copy(availability = TextToSpeechAvailability.Initializing) }
        engine =
            runCatching {
                TextToSpeech(appContext) { status -> handleInitResult(status) }
            }.getOrElse {
                markUnavailable()
                null
            }
    }

    private fun handleInitResult(status: Int) {
        val initialized = status == TextToSpeech.SUCCESS
        val languageResult =
            runCatching { engine?.setLanguage(locale) }
                .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                ?: TextToSpeech.LANG_NOT_SUPPORTED
        val languageSupported = initialized && languageResult.isSupportedLanguageResult()

        if (!languageSupported) {
            markUnavailable()
            return
        }

        mutableState.update { it.copy(availability = TextToSpeechAvailability.Ready) }
        speakPendingText()
    }

    private fun speakPendingText() {
        val text = pendingText ?: return
        pendingText = null

        val result =
            runCatching {
                engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, NAVIGATION_TTS_UTTERANCE_ID)
            }.getOrDefault(TextToSpeech.ERROR)

        if (result == TextToSpeech.ERROR) {
            markUnavailable()
        }
    }

    private fun markUnavailable() {
        pendingText = null
        mutableState.update { it.copy(availability = TextToSpeechAvailability.Unavailable) }
    }
}

object NoOpTextToSpeechController : TextToSpeechController {
    private val mutableState =
        MutableStateFlow(
            TextToSpeechState(
                enabled = false,
                availability = TextToSpeechAvailability.Unavailable,
            ),
        )
    override val state: StateFlow<TextToSpeechState> = mutableState.asStateFlow()

    override fun setEnabled(enabled: Boolean) = Unit

    override fun speak(text: String) = Unit

    override fun stop() = Unit

    override fun shutdown() = Unit
}

private const val NAVIGATION_TTS_UTTERANCE_ID = "navigation_guidance"

private fun Int.isSupportedLanguageResult(): Boolean =
    this != TextToSpeech.LANG_MISSING_DATA && this != TextToSpeech.LANG_NOT_SUPPORTED
