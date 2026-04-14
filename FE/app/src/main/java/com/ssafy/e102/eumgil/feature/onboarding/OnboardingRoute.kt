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
fun LocationTermsPlaceholderRoute(
    disabilityType: DisabilityType,
    disabilityLevel: DisabilityLevel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LocationTermsPlaceholderScreen(
        uiState = LocationTermsPlaceholderUiState(
            disabilityType = disabilityType,
            disabilityLevel = disabilityLevel,
        ),
        onBackClick = onNavigateBack,
        modifier = modifier,
    )
}
