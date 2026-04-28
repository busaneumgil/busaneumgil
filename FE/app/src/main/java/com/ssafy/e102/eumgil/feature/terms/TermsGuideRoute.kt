package com.ssafy.e102.eumgil.feature.terms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Route wrapper for [TermsGuideScreen].
 *
 * Holds the current step and the agreed flag in saveable state so configuration
 * changes (rotation, process recreation) keep the user in the same step. Caller
 * passes [onAgreed] / [onRequestDetails] / [onTabSelected] handlers; this layer
 * does not navigate by itself, matching the convention in feature/onboarding
 * (see DisabilityTypeRoute, LocationTermsRoute).
 */
@Composable
fun TermsGuideRoute(
    onAgreed: () -> Unit,
    onRequestDetails: () -> Unit,
    onTabSelected: (TermsBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    initialStep: Int = 1,
    totalSteps: Int = 5,
) {
    var currentStep by rememberSaveable(initialStep) { mutableStateOf(initialStep) }
    var isAgreed by rememberSaveable(initialStep) { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(TermsBottomTab.HOME) }

    val uiState = TermsGuideUiState(
        currentStep = currentStep,
        totalSteps = totalSteps,
        isAgreed = isAgreed,
    )

    TermsGuideScreen(
        uiState = uiState,
        onAgree = {
            if (!isAgreed) {
                isAgreed = true
                onAgreed()
            }
        },
        onMoreDetails = onRequestDetails,
        onTabSelected = { tab ->
            selectedTab = tab
            onTabSelected(tab)
        },
        selectedTab = selectedTab,
        modifier = modifier,
    )
}
