package com.ssafy.e102.eumgil.feature.route

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchSource
import com.ssafy.e102.eumgil.core.model.RouteWaypoint

data class RouteSettingUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val origin: RouteLocationUiState = RouteLocationUiState(),
    val destination: RouteLocationUiState = RouteLocationUiState(),
    val destinationHandoffState: RouteDestinationHandoffState = RouteDestinationHandoffState.EMPTY,
    val destinationFallbackMessage: String? = null,
    val isUsingFallbackDestination: Boolean = true,
    val selectedOption: RouteOption = RouteOption.SAFE,
    val optionCards: List<RouteOptionCardUiState> = emptyList(),
    val selectedRoute: RouteSelectedRouteUiState? = null,
    val routePreviewMap: RoutePreviewMapUiState = RoutePreviewMapUiState(),
    val sourceLabel: String? = null,
    val cta: RouteSettingCtaUiState = RouteSettingCtaUiState(),
    val ctaAcknowledged: Boolean = false,
) {
    val isStartEnabled: Boolean
        get() = cta.isEnabled
}

data class RouteLocationUiState(
    val placeId: String? = null,
    val name: String = "",
    val supportingText: String? = null,
    val coordinate: GeoCoordinate? = null,
    val category: PlaceCategory? = null,
    val metadataLabel: String? = null,
)

data class RoutePreviewMapUiState(
    val status: RoutePreviewMapStatus = RoutePreviewMapStatus.LOADING,
    val routeOption: RouteOption? = null,
    val originCoordinate: GeoCoordinate? = null,
    val destinationCoordinate: GeoCoordinate? = null,
    val polyline: List<GeoCoordinate> = emptyList(),
    val fallbackMessage: String? = null,
) {
    val isDisplayable: Boolean
        get() =
            status == RoutePreviewMapStatus.READY &&
                originCoordinate != null &&
                destinationCoordinate != null &&
                polyline.size >= 2
}

data class RouteOptionCardUiState(
    val routeOption: RouteOption,
    val title: String,
    val description: String,
    val distanceMeters: Int,
    val estimatedTimeMinutes: Int,
    val riskLevel: RouteRiskLevel,
    val summaryLabel: String,
    val selectionLabel: String,
    val highlightLabel: String? = null,
    val metrics: List<RouteOptionCardMetricUiState> = emptyList(),
    val badges: List<RouteOptionBadge> = emptyList(),
    val isSelected: Boolean = false,
)

data class RouteSelectedRouteUiState(
    val routeOption: RouteOption,
    val destination: RouteLocationUiState,
    val optionTitle: String,
    val title: String,
    val distanceMeters: Int,
    val estimatedTimeMinutes: Int,
    val riskLevel: RouteRiskLevel,
    val guidanceMessage: String,
    val summaryLabel: String,
    val estimatedTimeLabel: String,
    val distanceLabel: String,
    val riskLabel: String,
    val renderableSegmentLabel: String,
    val summaryMetrics: List<RouteSummaryMetricUiState> = emptyList(),
    val previewPoints: List<GeoCoordinate> = emptyList(),
    val segmentCount: Int = 0,
    val renderableSegmentCount: Int = 0,
    val fallbackSegmentCount: Int = 0,
    val previewFallbackNotice: String? = null,
    val badges: List<RouteOptionBadge> = emptyList(),
)

data class RouteSummaryMetricUiState(
    val label: String,
    val value: String,
)

data class RouteSettingCtaUiState(
    val label: String = "선택한 경로로 안내 시작",
    val supportingText: String = "fixture 기반 route summary를 불러오는 동안 CTA를 잠시 비활성화합니다.",
    val isEnabled: Boolean = false,
)

data class RouteOptionCardMetricUiState(
    val label: String,
    val value: String,
)

enum class RouteDestinationHandoffState {
    DIRECT,
    EMPTY,
    INVALID_COORDINATE,
}

enum class RoutePreviewMapStatus {
    LOADING,
    READY,
    NO_DESTINATION,
    INVALID_DESTINATION,
    NO_ROUTE,
    POLYLINE_UNAVAILABLE,
    ERROR,
}

enum class RouteOptionBadge {
    SAFE_PRIORITY,
    STEP_FREE,
    AUDIO_SIGNAL,
    BRAILLE_BLOCK,
    SIGNAL_CROSSWALK,
    CURB_GAP,
    UNSIGNALIZED_CROSSWALK,
}

sealed interface RouteSettingUiAction {
    data object BackClicked : RouteSettingUiAction

    data class RouteOptionSelected(
        val routeOption: RouteOption,
    ) : RouteSettingUiAction

    data object StartNavigationClicked : RouteSettingUiAction
}

sealed interface RouteSettingUiEvent {
    data object NavigateBack : RouteSettingUiEvent

    data class StartNavigationRequested(
        val request: RouteNavigationRequest,
    ) : RouteSettingUiEvent
}

data class RouteNavigationRequest(
    val origin: RouteWaypoint,
    val destination: RouteWaypoint,
    val selectedRoute: RouteCandidate,
    val source: RouteSearchSource,
)
