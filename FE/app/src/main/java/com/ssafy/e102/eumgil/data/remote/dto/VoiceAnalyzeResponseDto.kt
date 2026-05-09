package com.ssafy.e102.eumgil.data.remote.dto

data class VoiceAnalyzeResponseDto(
    val intent: String,
    val placeName: String?,
    val confirmed: Boolean?,
    val confirmationMessage: String?,
)
