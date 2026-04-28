package com.ssafy.e102.eumgil.feature.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ssafy.e102.eumgil.R

data class PrimaryUserTypeUiState(
    val selectedType: PrimaryUserType? = null,
)

data class MobilitySubtypeUiState(
    val selectedMobilitySubtype: MobilitySubtype? = null,
)

data class LocationTermsUiState(
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
            isLocationTermsAgreed = isLocationTermsChecked,
            isPrivacyPolicyAgreed = isPrivacyPolicyChecked,
        )
}

data class LocationTermsAgreement(
    val isLocationTermsAgreed: Boolean,
    val isPrivacyPolicyAgreed: Boolean,
)

enum class LocationTermsConsentStatus {
    PENDING,
    RESTRICTED,
    READY,
}

enum class PrimaryUserType(
    val routeValue: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val usesHighContrastCard: Boolean,
) {
    LOW_VISION(
        routeValue = "low_vision",
        titleRes = R.string.onboarding_primary_user_type_low_vision_title,
        descriptionRes = R.string.onboarding_primary_user_type_low_vision_description,
        iconRes = R.drawable.ic_user_visual_impairment,
        usesHighContrastCard = true,
    ),
    MOBILITY_IMPAIRED(
        routeValue = "mobility_impaired",
        titleRes = R.string.onboarding_primary_user_type_mobility_title,
        descriptionRes = R.string.onboarding_primary_user_type_mobility_description,
        iconRes = R.drawable.ic_user_wheelchair,
        usesHighContrastCard = false,
    ),
    ;

    companion object {
        fun fromRouteValue(routeValue: String?): PrimaryUserType? =
            entries.firstOrNull { it.routeValue == routeValue }
    }
}

enum class MobilitySubtype(
    val routeValue: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
) {
    ELECTRIC_WHEELCHAIR(
        routeValue = "electric_wheelchair",
        titleRes = R.string.onboarding_mobility_subtype_electric_title,
        descriptionRes = R.string.onboarding_mobility_subtype_electric_description,
        iconRes = R.drawable.ic_user_wheelchair,
    ),
    MANUAL_WHEELCHAIR(
        routeValue = "manual_wheelchair",
        titleRes = R.string.onboarding_mobility_subtype_manual_title,
        descriptionRes = R.string.onboarding_mobility_subtype_manual_description,
        iconRes = R.drawable.ic_user_wheelchair,
    ),
    OTHER(
        routeValue = "other_mobility_impaired",
        titleRes = R.string.onboarding_mobility_subtype_other_title,
        descriptionRes = R.string.onboarding_mobility_subtype_other_description,
        iconRes = R.drawable.ic_user_check,
    ),
    ;

    companion object {
        fun fromRouteValue(routeValue: String?): MobilitySubtype? =
            entries.firstOrNull { it.routeValue == routeValue }
    }
}
