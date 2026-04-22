package com.ssafy.e102.eumgil.feature.navigation

data class NavigationUiState(
    val screenState: NavigationScreenState = NavigationScreenState.Loading,
    val mapPlaceholderTitle: String = "실시간 지도 뷰",
    val mapPlaceholderDescription: String = "현재 위치와 경로 안내를 준비 중입니다.",
    val stepCard: NavigationStepCardUiState = navigationLoadingStepCardUiState(),
    val exitCta: NavigationCtaUiState = navigationLoadingCtaUiState(),
) {
    val isExitEnabled: Boolean
        get() = exitCta.isEnabled
}

enum class NavigationScreenState {
    Loading,
    Ready,
    Empty,
}

data class NavigationStepCardUiState(
    val sectionLabel: String = "다음 안내",
    val statusLabel: String = "준비 중",
    val emphasisLabel: String = "경로 확인",
    val distanceLabel: String = "확인 중",
    val instruction: String = "경로 안내를 불러오는 중입니다",
    val supportingText: String = "선택한 경로 정보를 확인한 뒤 첫 안내 메시지를 표시합니다.",
    val metrics: List<NavigationStepMetricUiState> =
        listOf(
            NavigationStepMetricUiState(
                label = "남은 거리",
                value = "확인 중",
            ),
            NavigationStepMetricUiState(
                label = "예상 시간",
                value = "확인 중",
            ),
            NavigationStepMetricUiState(
                label = "진행 단계",
                value = "-",
            ),
        ),
)

data class NavigationStepMetricUiState(
    val label: String,
    val value: String,
)

data class NavigationCtaUiState(
    val label: String = "안내 준비 중",
    val supportingText: String = "경로 정보가 준비되면 종료 버튼이 활성화됩니다.",
    val isEnabled: Boolean = false,
)

sealed interface NavigationUiAction {
    data object BackClicked : NavigationUiAction

    data object ExitNavigationClicked : NavigationUiAction
}

sealed interface NavigationUiEvent {
    data object NavigateBack : NavigationUiEvent

    data object NavigateToMap : NavigationUiEvent
}

private fun navigationLoadingStepCardUiState(): NavigationStepCardUiState = NavigationStepCardUiState()

private fun navigationLoadingCtaUiState(): NavigationCtaUiState = NavigationCtaUiState()
