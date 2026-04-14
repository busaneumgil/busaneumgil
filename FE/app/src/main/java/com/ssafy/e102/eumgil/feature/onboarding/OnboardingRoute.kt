package com.ssafy.e102.eumgil.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun DisabilityTypeRoute(
    onNavigateNext: (DisabilityType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTypeRoute by rememberSaveable { mutableStateOf<String?>(null) }

    DisabilityTypeScreen(
        uiState = DisabilityTypeUiState(
            selectedType = DisabilityType.fromRouteValue(selectedTypeRoute),
        ),
        onTypeSelected = { disabilityType ->
            selectedTypeRoute = disabilityType.routeValue
        },
        onNextClick = {
            DisabilityType.fromRouteValue(selectedTypeRoute)?.let(onNavigateNext)
        },
        modifier = modifier,
    )
}

@Composable
fun DisabilityLevelRoute(
    disabilityType: DisabilityType,
    onNavigateNext: (DisabilityLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedLevelRoute by rememberSaveable(disabilityType.routeValue) { mutableStateOf<String?>(null) }
    var isVoiceGuideExpanded by rememberSaveable(disabilityType.routeValue) {
        mutableStateOf(disabilityType.supportsVoiceGuide)
    }

    DisabilityLevelScreen(
        uiState = DisabilityLevelUiState(
            disabilityType = disabilityType,
            selectedLevel = DisabilityLevel.fromRouteValue(selectedLevelRoute),
            isVoiceGuideExpanded = isVoiceGuideExpanded,
        ),
        onLevelSelected = { disabilityLevel ->
            selectedLevelRoute = disabilityLevel.routeValue
        },
        onToggleVoiceGuide = {
            if (disabilityType.supportsVoiceGuide) {
                isVoiceGuideExpanded = !isVoiceGuideExpanded
            }
        },
        onNextClick = {
            DisabilityLevel.fromRouteValue(selectedLevelRoute)?.let(onNavigateNext)
        },
        modifier = modifier,
    )
}

@Composable
fun LocationTermsRoute(
    disabilityType: DisabilityType,
    disabilityLevel: DisabilityLevel,
    onConsentCompleted: (LocationTermsAgreement) -> Unit,
    onConsentDeferred: (LocationTermsAgreement) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLocationTermsChecked by rememberSaveable(
        disabilityType.routeValue,
        disabilityLevel.routeValue,
    ) { mutableStateOf(false) }
    var isPrivacyPolicyChecked by rememberSaveable(
        disabilityType.routeValue,
        disabilityLevel.routeValue,
    ) { mutableStateOf(false) }
    var hasRestrictionNotice by rememberSaveable(
        disabilityType.routeValue,
        disabilityLevel.routeValue,
    ) { mutableStateOf(false) }

    val uiState =
        LocationTermsUiState(
            disabilityType = disabilityType,
            disabilityLevel = disabilityLevel,
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
