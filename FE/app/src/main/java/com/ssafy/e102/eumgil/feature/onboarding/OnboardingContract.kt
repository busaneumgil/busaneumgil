package com.ssafy.e102.eumgil.feature.onboarding

import androidx.annotation.StringRes
import com.ssafy.e102.eumgil.R

data class DisabilityTypeUiState(
    val selectedType: DisabilityType? = null,
)

data class DisabilityLevelUiState(
    val disabilityType: DisabilityType,
    val selectedLevel: DisabilityLevel? = null,
    val isVoiceGuideExpanded: Boolean = false,
)

data class LocationTermsUiState(
    val disabilityType: DisabilityType,
    val disabilityLevel: DisabilityLevel,
    val isLocationTermsChecked: Boolean = false,
    val isPrivacyPolicyChecked: Boolean = false,
    val hasRestrictionNotice: Boolean = false,
) {
    val isAllTermsChecked: Boolean
        get() = isLocationTermsChecked && isPrivacyPolicyChecked

    val canProceed: Boolean
        get() = isLocationTermsChecked

    val consentStatus: LocationTermsConsentStatus
        get() = when {
            canProceed -> LocationTermsConsentStatus.READY
            hasRestrictionNotice -> LocationTermsConsentStatus.RESTRICTED
            else -> LocationTermsConsentStatus.PENDING
        }

    fun toAgreement(): LocationTermsAgreement =
        LocationTermsAgreement(
            disabilityType = disabilityType,
            disabilityLevel = disabilityLevel,
            isLocationTermsAgreed = isLocationTermsChecked,
            isPrivacyPolicyAgreed = isPrivacyPolicyChecked,
        )
}

data class LocationTermsAgreement(
    val disabilityType: DisabilityType,
    val disabilityLevel: DisabilityLevel,
    val isLocationTermsAgreed: Boolean,
    val isPrivacyPolicyAgreed: Boolean,
)

enum class LocationTermsConsentStatus {
    PENDING,
    RESTRICTED,
    READY,
}

enum class DisabilityType(
    val routeValue: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val modeTitleRes: Int,
    @StringRes val modeDescriptionRes: Int,
    val supportsVoiceGuide: Boolean,
) {
    VISUAL_IMPAIRMENT(
        routeValue = "visual",
        titleRes = R.string.onboarding_type_visual_title,
        descriptionRes = R.string.onboarding_type_visual_description,
        modeTitleRes = R.string.onboarding_mode_visual_title,
        modeDescriptionRes = R.string.onboarding_mode_visual_description,
        supportsVoiceGuide = true,
    ),
    MOBILITY_ASSISTANCE(
        routeValue = "mobility",
        titleRes = R.string.onboarding_type_mobility_title,
        descriptionRes = R.string.onboarding_type_mobility_description,
        modeTitleRes = R.string.onboarding_mode_mobility_title,
        modeDescriptionRes = R.string.onboarding_mode_mobility_description,
        supportsVoiceGuide = false,
    ),
    ;

    companion object {
        fun fromRouteValue(routeValue: String?): DisabilityType? =
            entries.firstOrNull { it.routeValue == routeValue }
    }
}

enum class DisabilityLevel(
    val routeValue: String,
    @StringRes val titleRes: Int,
    @StringRes val visualDescriptionRes: Int,
    @StringRes val mobilityDescriptionRes: Int,
) {
    SEVERE(
        routeValue = "severe",
        titleRes = R.string.onboarding_level_severe_title,
        visualDescriptionRes = R.string.onboarding_level_severe_visual_description,
        mobilityDescriptionRes = R.string.onboarding_level_severe_mobility_description,
    ),
    MILD(
        routeValue = "mild",
        titleRes = R.string.onboarding_level_mild_title,
        visualDescriptionRes = R.string.onboarding_level_mild_visual_description,
        mobilityDescriptionRes = R.string.onboarding_level_mild_mobility_description,
    ),
    NONE(
        routeValue = "none",
        titleRes = R.string.onboarding_level_none_title,
        visualDescriptionRes = R.string.onboarding_level_none_visual_description,
        mobilityDescriptionRes = R.string.onboarding_level_none_mobility_description,
    ),
    ;

    @StringRes
    fun descriptionRes(disabilityType: DisabilityType): Int =
        if (disabilityType.supportsVoiceGuide) {
            visualDescriptionRes
        } else {
            mobilityDescriptionRes
        }

    companion object {
        fun fromRouteValue(routeValue: String?): DisabilityLevel? =
            entries.firstOrNull { it.routeValue == routeValue }
    }
}
