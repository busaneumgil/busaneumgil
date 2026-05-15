package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.feature.route.RouteDetailStepKind
import com.ssafy.e102.eumgil.feature.route.RouteTransitOptionLabelUiState
import com.ssafy.e102.eumgil.feature.route.toRouteDetailStepKind

data class NavigationUiState(
    val screenState: NavigationScreenState = NavigationScreenState.Loading,
    val selectedRouteOption: RouteOption? = null,
    val mapPlaceholderTitle: String = "Navigation map",
    val mapPlaceholderDescription: String = "Preparing route guidance.",
    val mapOverlay: NavigationMapOverlayUiState = NavigationMapOverlayUiState(),
    val segmentSync: NavigationSegmentSyncUiState = NavigationSegmentSyncUiState(),
    val focusedSegmentCard: NavigationFocusedSegmentCardUiState? = null,
    val pendingActiveChangeLabel: String? = null,
    val stepCard: NavigationStepCardUiState = navigationLoadingStepCardUiState(),
    val exitCta: NavigationCtaUiState = navigationLoadingCtaUiState(),
    val isExitConfirmDialogVisible: Boolean = false,
    val tts: NavigationTtsUiState = NavigationTtsUiState(),
) {
    val isExitEnabled: Boolean
        get() = exitCta.isEnabled

    val canOpenRouteDetail: Boolean
        get() = selectedRouteOption != null && screenState != NavigationScreenState.Loading

    val remainingDistanceLabel: String
        get() = stepCard.metrics.getOrNull(0)?.value ?: "확인 중"

    val remainingEtaLabel: String
        get() = stepCard.metrics.getOrNull(1)?.value ?: "확인 중"

    val progressLabel: String
        get() = stepCard.metrics.getOrNull(2)?.value ?: "-"
}

enum class NavigationScreenState {
    Loading,
    Ready,
    Empty,
}

enum class NavigationGuidanceAction(
    val label: String,
) {
    BUS("버스 탑승"),
    SUBWAY("지하철 탑승"),
    STRAIGHT("직진"),
    TURN_LEFT("좌회전"),
    TURN_RIGHT("우회전"),
    CROSSWALK("횡단보도"),
}

data class NavigationMapOverlayUiState(
    val isDisplayable: Boolean = false,
    val currentLocation: NavigationMapPointUiState? = null,
    val origin: NavigationMapPointUiState? = null,
    val destination: NavigationMapPointUiState? = null,
    val selectedRoutePolyline: List<GeoCoordinate> = emptyList(),
    val activeSegmentPolyline: List<GeoCoordinate> = emptyList(),
    val focusedSegmentPolyline: List<GeoCoordinate> = emptyList(),
    val activeSegmentTravelKind: NavigationSegmentTravelKind = NavigationSegmentTravelKind.WALK,
    val focusedSegmentTravelKind: NavigationSegmentTravelKind = NavigationSegmentTravelKind.WALK,
    val focusCoordinate: GeoCoordinate? = null,
    val routeSegments: List<NavigationMapSegmentUiState> = emptyList(),
    val mapFocusMode: NavigationMapFocusMode = NavigationMapFocusMode.ACTIVE,
    val shouldAnimateCameraTransition: Boolean = true,
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
    val segmentStartCoordinate: GeoCoordinate? = null,
    val distanceMeters: Int,
    val riskLevel: RouteRiskLevel,
    val guidanceMessage: String,
    val travelKind: NavigationSegmentTravelKind = NavigationSegmentTravelKind.WALK,
    val isActive: Boolean = false,
    val isFocused: Boolean = false,
    val isCompleted: Boolean = false,
    val isRiskUpcoming: Boolean = false,
    val showJunctionMarker: Boolean = true,
) {
    val isRenderable: Boolean
        get() = polyline.size >= 2
}

enum class NavigationSegmentTravelKind {
    WALK,
    TRANSIT,
}

data class NavigationSegmentSyncUiState(
    val activeSegmentIndex: Int = 0,
    val focusedSegmentIndex: Int = 0,
    val isInspectingSegments: Boolean = false,
    val mapFocusMode: NavigationMapFocusMode = NavigationMapFocusMode.ACTIVE,
    val hasPendingActiveChange: Boolean = false,
    val railItems: List<NavigationSegmentRailItemUiState> = emptyList(),
)

data class NavigationSegmentRailItemUiState(
    val index: Int,
    val sequence: Int,
    val instruction: String,
    val distanceLabel: String,
    val riskLabel: String,
    val guidanceAction: NavigationGuidanceAction = NavigationGuidanceAction.STRAIGHT,
    val isActive: Boolean = false,
    val isFocused: Boolean = false,
    val isCompleted: Boolean = false,
    val isRiskUpcoming: Boolean = false,
)

data class NavigationFocusedSegmentCardUiState(
    val sequenceLabel: String,
    val instruction: String,
    val heroTitle: String,
    val heroDescription: String,
    val distanceLabel: String,
    val riskLabel: String,
    val supportingText: String,
    val guidanceAction: NavigationGuidanceAction = NavigationGuidanceAction.STRAIGHT,
    val transitInfo: NavigationTransitInfoUiState? = null,
)

enum class NavigationMapFocusMode {
    ACTIVE,
    FOCUSED,
}

data class NavigationStepCardUiState(
    val sectionLabel: String = "다음 안내",
    val statusLabel: String = "준비 중",
    val emphasisLabel: String = "경로 확인",
    val distanceLabel: String = "확인 중",
    val heroTitle: String = "경로 안내",
    val heroDescription: String = "현재 구간의 이동 정보를 확인하고 있습니다.",
    val instruction: String = "경로 안내를 준비하고 있습니다",
    val supportingText: String = "현재 위치를 확인한 뒤 안내를 시작합니다.",
    val guidanceAction: NavigationGuidanceAction = NavigationGuidanceAction.STRAIGHT,
    val transitInfo: NavigationTransitInfoUiState? = null,
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

data class NavigationTransitInfoUiState(
    val guidanceAction: NavigationGuidanceAction,
    val startName: String,
    val endName: String,
    val durationLabel: String? = null,
    val optionLabels: List<RouteTransitOptionLabelUiState> = emptyList(),
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

    data object RouteDetailClicked : NavigationUiAction

    data object ExitNavigationClicked : NavigationUiAction

    data object ExitNavigationDismissed : NavigationUiAction

    data object ConfirmExitNavigationClicked : NavigationUiAction

    data object SaveBookmarkClicked : NavigationUiAction

    data object NavigationCompleteClicked : NavigationUiAction

    data class SegmentTapped(
        val index: Int,
    ) : NavigationUiAction

    data object ReturnToActiveSegmentClicked : NavigationUiAction

    data class VoiceGuidanceToggled(
        val enabled: Boolean,
    ) : NavigationUiAction

    data object BriefingReplayClicked : NavigationUiAction

    data object StopBriefingClicked : NavigationUiAction
}

sealed interface NavigationUiEvent {
    data object NavigateBack : NavigationUiEvent

    data class NavigateToRouteDetail(
        val routeOption: RouteOption,
    ) : NavigationUiEvent

    data object NavigateToMap : NavigationUiEvent

    data object NavigateToSavedRoute : NavigationUiEvent

    data object NavigateToArrival : NavigationUiEvent

    data class SpeakBriefing(
        val text: String,
    ) : NavigationUiEvent

    data object PlayRouteChangeAlert : NavigationUiEvent

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
        get() = isEnabled &&
            canSpeak &&
            status == NavigationTtsStatus.Ready &&
            briefingText.isNotBlank()
}

enum class NavigationTtsStatus {
    Initializing,
    Ready,
    Unavailable,
}

internal fun RouteSegment.toNavigationGuidanceAction(): NavigationGuidanceAction =
    toRouteDetailStepKind().toNavigationGuidanceAction()

internal fun RouteCandidate.toNavigationGuidanceAction(segment: RouteSegment): NavigationGuidanceAction =
    toRouteDetailStepKind(segment).toNavigationGuidanceAction()

internal fun RouteDetailStepKind.toNavigationGuidanceAction(): NavigationGuidanceAction =
    when {
        this == RouteDetailStepKind.BUS -> NavigationGuidanceAction.BUS
        this == RouteDetailStepKind.SUBWAY -> NavigationGuidanceAction.SUBWAY
        this == RouteDetailStepKind.CROSSWALK -> NavigationGuidanceAction.CROSSWALK
        this == RouteDetailStepKind.TURN_LEFT -> NavigationGuidanceAction.TURN_LEFT
        this == RouteDetailStepKind.TURN_RIGHT -> NavigationGuidanceAction.TURN_RIGHT
        else -> NavigationGuidanceAction.STRAIGHT
    }

const val NAVIGATION_TTS_PREPARING_MESSAGE: String = "음성 안내를 준비하고 있습니다."
const val NAVIGATION_TTS_UNAVAILABLE_MESSAGE: String = "이 기기에서는 음성 안내를 사용할 수 없습니다."
const val NAVIGATION_TTS_DISABLED_MESSAGE: String = "음성 안내가 꺼져 있습니다."
