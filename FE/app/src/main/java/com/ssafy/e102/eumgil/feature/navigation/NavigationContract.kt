package com.ssafy.e102.eumgil.feature.navigation

data class NavigationUiState(
    val currentInstruction: String = "120m 앞에서 오른쪽으로 이동하세요",
    val remainingDistanceText: String = "1.2km",
    val estimatedTimeText: String = "약 18분",
    val currentStepIndex: Int = 1,
    val totalStepCount: Int = 5,
    val mapPlaceholderTitle: String = "실시간 지도 뷰",
    val mapPlaceholderDescription: String = "현재 위치와 경로 오버레이가 이 영역에 연결될 예정입니다.",
    val stepCardPlaceholderTitle: String = "현재 안내 카드",
    val stepCardPlaceholderDescription: String = "step card 상세 UI는 다음 작업에서 연결합니다.",
    val exitCta: NavigationCtaUiState = NavigationCtaUiState(),
) {
    val progressText: String
        get() = "$currentStepIndex / $totalStepCount"

    val isExitEnabled: Boolean
        get() = exitCta.isEnabled
}

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
