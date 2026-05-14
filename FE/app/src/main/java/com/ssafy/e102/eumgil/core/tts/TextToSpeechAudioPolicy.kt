package com.ssafy.e102.eumgil.core.tts

internal data class TextToSpeechAudioConfig(
    val usage: Int,
    val contentType: Int,
    val focusGain: Int,
)

internal fun defaultTextToSpeechAudioConfig(): TextToSpeechAudioConfig =
    TextToSpeechAudioConfig(
        usage = USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
        contentType = CONTENT_TYPE_SPEECH,
        focusGain = AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
    )

private const val USAGE_ASSISTANCE_NAVIGATION_GUIDANCE = 12
private const val CONTENT_TYPE_SPEECH = 1
private const val AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3
