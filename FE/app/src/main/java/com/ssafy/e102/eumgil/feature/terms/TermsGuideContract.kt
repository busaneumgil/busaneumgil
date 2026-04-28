package com.ssafy.e102.eumgil.feature.terms

/**
 * Terms guide screen UI state.
 *
 * Mirrors Figma node 328:486 (E102-mockup, file MREqSzkmwhRcXnFS3lzW17): a five-step
 * walkthrough with the first step focused on the "약관 동의" double-tap interaction.
 */
data class TermsGuideUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 5,
    val isAgreed: Boolean = false,
) {
    val pageIndex: Int
        get() = (currentStep - 1).coerceIn(0, totalSteps - 1)
}

/**
 * Bottom navigation tabs rendered at the bottom of the high-contrast walkthrough.
 *
 * Selection here only highlights the tab; tab routing is delegated to the caller because
 * the destinations live under [com.ssafy.e102.eumgil.app.navigation.TopLevelDestination]
 * and are not all reachable while onboarding is incomplete.
 */
enum class TermsBottomTab {
    HOME,
    BOOKMARK,
    CATEGORY,
    MY,
}
