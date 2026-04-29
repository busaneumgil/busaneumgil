package com.example.llmtest.network.models

data class VoiceAnalyzeResponse(
    val success: Boolean,
    val intent: String?,
    val place_name: String?,
    val confirmation_message: String?,
    val model: String?,
    val mode: String?,
    val latency_ms: Int?,
    val error: String?
)
