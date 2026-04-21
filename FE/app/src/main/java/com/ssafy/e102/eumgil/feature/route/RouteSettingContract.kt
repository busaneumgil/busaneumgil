package com.ssafy.e102.eumgil.feature.route

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
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
    val isUsingFallbackDestination: Boolean = true,
    val selectedOption: RouteOption = RouteOption.SAFE,
    val optionCards: List<RouteOptionCardUiState> = emptyList(),
    val selectedRoute: RouteSelectedRouteUiState? = null,
    val sourceLabel: String? = null,
    val ctaAcknowledged: Boolean = false,
) {
    val isStartEnabled: Boolean
        get() = !isLoading && loadErrorMessage == null && selectedRoute != null
}

data class RouteLocationUiState(
    val name: String = "",
    val supportingText: String? = null,
    val coordinate: GeoCoordinate? = null,
)

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

data class RouteOptionCardMetricUiState(
    val label: String,
    val value: String,
)

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
