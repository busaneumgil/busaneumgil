package com.ssafy.e102.eumgil.feature.navigation

data class NavigationUiState(
    val screenState: NavigationScreenState = NavigationScreenState.Guiding,
    val currentInstruction: String = "120m 앞에서 오른쪽으로 이동하세요.",
    val remainingDistanceText: String = "1.2km",
    val estimatedTimeText: String = "약 18분",
    val currentStepIndex: Int = 1,
    val totalStepCount: Int = 5,
    val isVoiceGuidanceEnabled: Boolean = true,
    val isRerouting: Boolean = false,
    val errorMessage: String? = null,
) {
    val progressText: String
        get() = "$currentStepIndex / $totalStepCount"
}

enum class NavigationScreenState {
    Preparing,
    Guiding,
    Rerouting,
    Completed,
    Failure,
}

sealed interface NavigationUiAction {
    data object BackClicked : NavigationUiAction

    data object EndNavigationClicked : NavigationUiAction

    data object VoiceGuidanceToggled : NavigationUiAction

    data object RerouteClicked : NavigationUiAction

    data object CompleteMockNavigationClicked : NavigationUiAction

    data object RetryClicked : NavigationUiAction
}

sealed interface NavigationUiEvent {
    data object NavigateBack : NavigationUiEvent

    data object NavigateToMap : NavigationUiEvent

    data class ShowSnackbar(
        val message: String,
    ) : NavigationUiEvent
}
