package com.ssafy.e102.eumgil.data.repository

internal const val ROUTE_PRIMARY_USER_TYPE_LOW_VISION: String = "low_vision"
internal const val ROUTE_PRIMARY_USER_TYPE_MOBILITY_IMPAIRED: String = "mobility_impaired"

private const val ROUTE_MOBILITY_SUBTYPE_ELECTRIC_WHEELCHAIR: String = "electric_wheelchair"
private const val ROUTE_MOBILITY_SUBTYPE_MANUAL_WHEELCHAIR: String = "manual_wheelchair"
private const val ROUTE_MOBILITY_SUBTYPE_OTHER: String = "other_mobility_impaired"

private const val SERVER_PRIMARY_USER_TYPE_LOW_VISION: String = "LOW_VISION"
private const val SERVER_PRIMARY_USER_TYPE_MOBILITY_IMPAIRED: String = "MOBILITY_IMPAIRED"

private const val SERVER_MOBILITY_SUBTYPE_POWER_WHEELCHAIR: String = "POWER_WHEELCHAIR"
private const val SERVER_MOBILITY_SUBTYPE_MANUAL_WHEELCHAIR: String = "MANUAL_WHEELCHAIR"
private const val SERVER_MOBILITY_SUBTYPE_OTHER: String = "OTHER_MOBILITY"

internal fun String.toPrimaryUserTypeRouteValue(): String =
    when (this) {
        SERVER_PRIMARY_USER_TYPE_LOW_VISION -> ROUTE_PRIMARY_USER_TYPE_LOW_VISION
        SERVER_PRIMARY_USER_TYPE_MOBILITY_IMPAIRED -> ROUTE_PRIMARY_USER_TYPE_MOBILITY_IMPAIRED
        else -> throw IllegalStateException("지원하지 않는 사용자 유형입니다.")
    }

internal fun String.toMobilitySubtypeRouteValue(): String =
    when (this) {
        SERVER_MOBILITY_SUBTYPE_POWER_WHEELCHAIR -> ROUTE_MOBILITY_SUBTYPE_ELECTRIC_WHEELCHAIR
        SERVER_MOBILITY_SUBTYPE_MANUAL_WHEELCHAIR -> ROUTE_MOBILITY_SUBTYPE_MANUAL_WHEELCHAIR
        SERVER_MOBILITY_SUBTYPE_OTHER -> ROUTE_MOBILITY_SUBTYPE_OTHER
        else -> throw IllegalStateException("지원하지 않는 보행약자 세부 유형입니다.")
    }

internal fun String.toPrimaryUserTypeServerValue(): String =
    when (this) {
        ROUTE_PRIMARY_USER_TYPE_LOW_VISION -> SERVER_PRIMARY_USER_TYPE_LOW_VISION
        ROUTE_PRIMARY_USER_TYPE_MOBILITY_IMPAIRED -> SERVER_PRIMARY_USER_TYPE_MOBILITY_IMPAIRED
        else -> throw IllegalStateException("지원하지 않는 사용자 유형입니다.")
    }

internal fun String.toMobilitySubtypeServerValue(): String =
    when (this) {
        ROUTE_MOBILITY_SUBTYPE_ELECTRIC_WHEELCHAIR -> SERVER_MOBILITY_SUBTYPE_POWER_WHEELCHAIR
        ROUTE_MOBILITY_SUBTYPE_MANUAL_WHEELCHAIR -> SERVER_MOBILITY_SUBTYPE_MANUAL_WHEELCHAIR
        ROUTE_MOBILITY_SUBTYPE_OTHER -> SERVER_MOBILITY_SUBTYPE_OTHER
        else -> throw IllegalStateException("지원하지 않는 보행약자 세부 유형입니다.")
    }

internal suspend fun SettingsRepository.syncOnboardingStateFromServer(
    selectedPrimaryUserType: String,
    selectedMobilitySubtype: String?,
) {
    savePrimaryUserType(selectedPrimaryUserType.toPrimaryUserTypeRouteValue())
    if (selectedPrimaryUserType == SERVER_PRIMARY_USER_TYPE_MOBILITY_IMPAIRED) {
        val mobilitySubtype =
            selectedMobilitySubtype
                ?: throw IllegalStateException("보행약자 세부 유형을 받지 못했습니다.")
        saveMobilitySubtype(mobilitySubtype.toMobilitySubtypeRouteValue())
    }
    saveLowVisionFollowUpCompleted(
        isCompleted = selectedPrimaryUserType == SERVER_PRIMARY_USER_TYPE_LOW_VISION,
    )
    saveLocationTermsAgreement(
        isLocationTermsAgreed = true,
        isPrivacyPolicyAgreed = true,
    )
}
