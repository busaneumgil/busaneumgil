package com.ssafy.e102.eumgil.feature.map

import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerFilterUiState
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerOverlayState

data class MapUiState(
    val cameraTarget: MapCameraTarget = MapCameraTarget.DefaultBusan,
    val selectedDestination: PlaceDestination? = null,
    val selectedMarkerId: String? = null,
    val locationStatus: MapLocationStatus = MapLocationStatus.PermissionDenied,
    val recenterButtonState: MapRecenterButtonState = MapRecenterButtonState.REQUEST_PERMISSION,
    val markerOverlayState: MapMarkerOverlayState = MapMarkerOverlayState(),
    val markerFilterState: MapMarkerFilterUiState = MapMarkerFilterUiState(),
    val facilityDetailSheetState: MapFacilityDetailSheetState = MapFacilityDetailSheetState(),
)

data class MapFacilityDetailSheetState(
    val detail: FacilityDetailSeed? = null,
) {
    val isVisible: Boolean
        get() = detail != null
}

sealed interface MapUiAction {
    data object SearchEntryClicked : MapUiAction

    data object LocationActionClicked : MapUiAction

    data object FacilityDetailDismissed : MapUiAction

    data object FacilityRouteEntryClicked : MapUiAction

    data class MarkerTapped(
        val markerId: String,
    ) : MapUiAction

    data class MarkerCategoryFilterToggled(
        val category: FacilityCategory,
    ) : MapUiAction

    data object MarkerCategoryFilterReset : MapUiAction
}

sealed interface MapUiEvent {
    data object NavigateToSearch : MapUiEvent

    data object NavigateToFacilityRouteEntry : MapUiEvent

    data object RequestLocationPermission : MapUiEvent
}

sealed interface MapLocationStatus {
    data object PermissionDenied : MapLocationStatus

    data object Loading : MapLocationStatus

    data class Ready(
        val location: MapCoordinate,
        val accuracyMeters: Float?,
    ) : MapLocationStatus

    data class Unavailable(
        val reason: MapLocationUnavailableReason,
    ) : MapLocationStatus
}

enum class MapLocationUnavailableReason {
    CURRENT_LOCATION_UNAVAILABLE,
    LOCATION_SERVICES_DISABLED,
    NO_LOCATION_FEATURE,
}

enum class MapRecenterButtonState {
    REQUEST_PERMISSION,
    LOADING,
    RETRY,
    DISABLED,
    ENABLED,
}
