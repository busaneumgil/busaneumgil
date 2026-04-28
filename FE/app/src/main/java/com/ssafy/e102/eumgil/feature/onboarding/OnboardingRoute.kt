package com.ssafy.e102.eumgil.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun PrimaryUserTypeRoute(
    initialSelectedType: PrimaryUserType? = null,
    onNavigateNext: (PrimaryUserType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTypeRoute by rememberSaveable(initialSelectedType?.routeValue) {
        mutableStateOf(initialSelectedType?.routeValue)
    }

    PrimaryUserTypeScreen(
        uiState = PrimaryUserTypeUiState(selectedType = PrimaryUserType.fromRouteValue(selectedTypeRoute)),
        onTypeSelected = { primaryUserType ->
            selectedTypeRoute = primaryUserType.routeValue
        },
        onNextClick = {
            PrimaryUserType.fromRouteValue(selectedTypeRoute)?.let(onNavigateNext)
        },
        modifier = modifier,
    )
}

@Composable
fun LowVisionFollowUpRoute(
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LowVisionFollowUpScreen(
        onNextClick = onNavigateNext,
        modifier = modifier,
    )
}

@Composable
fun MobilityTypeSecondaryRoute(
    initialSelectedSubtype: MobilitySubtype? = null,
    onNavigateBack: () -> Unit,
    onNavigateNext: (MobilitySubtype) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMobilitySubtypeRoute by rememberSaveable(initialSelectedSubtype?.routeValue) {
        mutableStateOf(initialSelectedSubtype?.routeValue)
    }

    MobilitySubtypeScreen(
        uiState =
            MobilitySubtypeUiState(
                selectedMobilitySubtype = MobilitySubtype.fromRouteValue(selectedMobilitySubtypeRoute),
            ),
        onSubtypeSelected = { mobilitySubtype ->
            selectedMobilitySubtypeRoute = mobilitySubtype.routeValue
        },
        onNavigateBack = onNavigateBack,
        onNextClick = {
            MobilitySubtype.fromRouteValue(selectedMobilitySubtypeRoute)?.let(onNavigateNext)
        },
        modifier = modifier,
    )
}

@Composable
fun LocationTermsRoute(
    initialLocationTermsChecked: Boolean = false,
    initialPrivacyPolicyChecked: Boolean = false,
    onConsentCompleted: (LocationTermsAgreement) -> Unit,
    onConsentDeferred: (LocationTermsAgreement) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLocationTermsChecked by rememberSaveable(initialLocationTermsChecked) {
        mutableStateOf(initialLocationTermsChecked)
    }
    var isPrivacyPolicyChecked by rememberSaveable(initialPrivacyPolicyChecked) {
        mutableStateOf(initialPrivacyPolicyChecked)
    }
    var hasRestrictionNotice by rememberSaveable { mutableStateOf(false) }

    val uiState =
        LocationTermsUiState(
            isLocationTermsChecked = isLocationTermsChecked,
            isPrivacyPolicyChecked = isPrivacyPolicyChecked,
            hasRestrictionNotice = hasRestrictionNotice,
        )

    LocationTermsScreen(
        uiState = uiState,
        onAllTermsCheckedChange = { shouldCheckAll ->
            isLocationTermsChecked = shouldCheckAll
            isPrivacyPolicyChecked = shouldCheckAll
            if (shouldCheckAll) {
                hasRestrictionNotice = false
            }
        },
        onLocationTermsCheckedChange = { isChecked ->
            isLocationTermsChecked = isChecked
            if (isChecked) {
                hasRestrictionNotice = false
            }
        },
        onPrivacyPolicyCheckedChange = { isChecked ->
            isPrivacyPolicyChecked = isChecked
        },
        onPrimaryActionClick = {
            if (uiState.canProceed) {
                onConsentCompleted(uiState.toAgreement())
            }
        },
        onSecondaryActionClick = {
            hasRestrictionNotice = true
            onConsentDeferred(uiState.toAgreement())
        },
        modifier = modifier,
    )
}
