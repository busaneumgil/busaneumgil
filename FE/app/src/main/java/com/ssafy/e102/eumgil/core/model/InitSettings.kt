package com.ssafy.e102.eumgil.core.model

data class InitSettings(
    val disabilityType: String? = null,
    val disabilityLevel: String? = null,
    val isLocationTermsAgreed: Boolean = false,
    val isPrivacyPolicyAgreed: Boolean = false,
) {
    val isOnboardingCompleted: Boolean
        get() = disabilityType != null && disabilityLevel != null && isLocationTermsAgreed
}
