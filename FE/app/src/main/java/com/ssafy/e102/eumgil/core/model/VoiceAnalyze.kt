package com.ssafy.e102.eumgil.core.model

enum class VoiceAnalyzeMode {
    MOBILITY_IMPAIRED,
    LOW_VISION,
}

enum class VoiceAnalyzeIntent {
    PLACE_SEARCH,
    UNKNOWN,
}

data class VoiceAnalyzeHistoryItem(
    val role: String,
    val content: String,
)

data class VoiceAnalyzeResult(
    val intent: VoiceAnalyzeIntent,
    val placeName: String?,
    val confirmed: Boolean?,
    val confirmationMessage: String?,
)
