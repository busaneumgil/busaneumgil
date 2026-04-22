package com.ssafy.e102.eumgil.feature.navigation

data class NavigationUiState(
    val mapPlaceholderTitle: String = "실시간 지도 뷰",
    val mapPlaceholderDescription: String = "현재 위치와 경로 오버레이가 이 영역에 연결될 예정입니다.",
    val stepCard: NavigationStepCardUiState = NavigationStepCardUiState(),
    val exitCta: NavigationCtaUiState = NavigationCtaUiState(),
) {
    val isExitEnabled: Boolean
        get() = exitCta.isEnabled
}

data class NavigationStepCardUiState(
    val sectionLabel: String = "다음 안내",
    val statusLabel: String = "안내 중",
    val emphasisLabel: String = "우회전",
    val distanceLabel: String = "120m",
    val instruction: String = "120m 앞에서 오른쪽 보행로로 이동하세요",
    val supportingText: String = "횡단보도를 지난 뒤 점자블록을 따라 우측 보행로로 진입합니다.",
    val metrics: List<NavigationStepMetricUiState> =
        listOf(
            NavigationStepMetricUiState(
                label = "남은 거리",
                value = "1.2km",
            ),
            NavigationStepMetricUiState(
                label = "예상 시간",
                value = "약 18분",
            ),
            NavigationStepMetricUiState(
                label = "진행 단계",
                value = "1 / 5",
            ),
        ),
)

data class NavigationStepMetricUiState(
    val label: String,
    val value: String,
)

data class NavigationCtaUiState(
    val label: String = "내비게이션 종료",
    val supportingText: String = "종료 플로우는 후속 작업에서 연결합니다.",
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
