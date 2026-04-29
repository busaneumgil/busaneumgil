package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel

data class NavigationUiState(
    val screenState: NavigationScreenState = NavigationScreenState.Loading,
    val mapPlaceholderTitle: String = "실시간 지도 뷰",
    val mapPlaceholderDescription: String = "현재 위치와 경로 안내를 준비 중입니다.",
    val mapOverlay: NavigationMapOverlayUiState = NavigationMapOverlayUiState(),
    val stepCard: NavigationStepCardUiState = navigationLoadingStepCardUiState(),
    val exitCta: NavigationCtaUiState = navigationLoadingCtaUiState(),
    val tts: NavigationTtsUiState = NavigationTtsUiState(),
) {
    val isExitEnabled: Boolean
        get() = exitCta.isEnabled
}

enum class NavigationScreenState {
    Loading,
    Ready,
    Empty,
}

data class NavigationMapOverlayUiState(
    val isDisplayable: Boolean = false,
    val currentLocation: NavigationMapPointUiState? = null,
    val origin: NavigationMapPointUiState? = null,
    val destination: NavigationMapPointUiState? = null,
    val selectedRoutePolyline: List<GeoCoordinate> = emptyList(),
    val routeSegments: List<NavigationMapSegmentUiState> = emptyList(),
) {
    val shouldUsePlaceholder: Boolean
        get() = !isDisplayable
}

data class NavigationMapPointUiState(
    val label: String,
    val coordinate: GeoCoordinate,
)

data class NavigationMapSegmentUiState(
    val sequence: Int,
    val polyline: List<GeoCoordinate>,
    val distanceMeters: Int,
    val riskLevel: RouteRiskLevel,
    val guidanceMessage: String,
) {
    val isRenderable: Boolean
        get() = polyline.size >= 2
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
    data object NavigationEntered : NavigationUiAction

    data object BackClicked : NavigationUiAction

    data object ExitNavigationClicked : NavigationUiAction

    /** 안내 종료 후 현재 경로를 북마크에 저장하고 저장 목록 화면으로 이동합니다. */
    data object SaveBookmarkClicked : NavigationUiAction

    /** 안내를 완전히 종료하고 홈(지도) 화면으로 돌아갑니다. */
    data object NavigationCompleteClicked : NavigationUiAction

    data class VoiceGuidanceToggled(
        val enabled: Boolean,
    ) : NavigationUiAction

    data object BriefingReplayClicked : NavigationUiAction

    data object StopBriefingClicked : NavigationUiAction
}

sealed interface NavigationUiEvent {
    data object NavigateBack : NavigationUiEvent

    data object NavigateToMap : NavigationUiEvent

    /** 북마크 저장 후 저장 목록(SavedRoute) 화면으로 이동합니다. */
    data object NavigateToSavedRoute : NavigationUiEvent

    data class SpeakBriefing(
        val text: String,
    ) : NavigationUiEvent

    data object StopBriefing : NavigationUiEvent

    data class SetVoiceGuidanceEnabled(
        val enabled: Boolean,
    ) : NavigationUiEvent
}

private fun navigationLoadingStepCardUiState(): NavigationStepCardUiState = NavigationStepCardUiState()

private fun navigationLoadingCtaUiState(): NavigationCtaUiState = NavigationCtaUiState()

data class NavigationTtsUiState(
    val isEnabled: Boolean = true,
    val canSpeak: Boolean = false,
    val status: NavigationTtsStatus = NavigationTtsStatus.Initializing,
    val briefingText: String = "",
    val fallbackMessage: String = NAVIGATION_TTS_PREPARING_MESSAGE,
) {
    val canRequestBriefing: Boolean
        get() = isEnabled && status !=