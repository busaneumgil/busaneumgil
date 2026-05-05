package com.ssafy.e102.eumgil.data.remote.dto

data class SocialLoginResponseDto(
    val signupRequired: Boolean,
    val signupToken: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val userId: String?,
    val selectedPrimaryUserType: String?,
    val selectedMobilitySubtype: String?,
)
