package com.ssafy.e102.eumgil.feature.map.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ssafy.e102.eumgil.BuildConfig
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerLoadStatus
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerOverlayState
import kotlin.math.max

@Composable
internal fun MapOverlayViewport(
    overlayState: MapViewportOverlayState,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onMarkerClick: (String) -> Unit = {},
) {
    val describedModifier =
        if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        }
    val integrationState =
        resolveMapIntegrationState(
            hasNativeAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank(),
            isInspectionMode = LocalInspectionMode.current,
        )

    when (integrationState) {
        is MapIntegrationState.Bound ->
            KakaoMapViewport(
                state =
                    MapViewportUiState(
                        integrationState = integrationState,
                        cameraTarget = overlayState.toMapCameraTarget(),
                        currentLocation = null,
                        selectedDestinationCoordinate = null,
                        selectedDestinationName = null,
                        markerOverlayState = MapMarkerOverlayState(loadStatus = MapMarkerLoadStatus.READY),
                        overlayState = overlayState,
                        selectedMarkerId = null,
                        selectedMapPinCoordinate = null,
                        regionLabel = "",
                        statusLabel = "",
                        title = "",
                        description = "",
                        supportingText = "",
                    ),
                onMarkerClick = onMarkerClick,
                onCameraMoveEnd = { _, _, _, _ -> },
                onMapClick = {},
                modifier = describedModifier,
            )

        MapIntegrationState.Unbound ->
            MapViewportOverlayBackdrop(
                overlayState = overlayState,
                modifier = describedModifier,
                onPointClick = onMarkerClick,
            )
    }
}

private fun MapViewportOverlayState.toMapCameraTarget(): MapCameraTarget {
    val points = projectionCoordinates()
    if (points.isEmpty()) {
        return MapCameraTarget(
            center = fallbackCamera.center,
            source = MapCameraSource.SEARCH_RESULT,
            requestId = hashCode().toLong(),
            zoomLevel = fallbackCamera.toApproximateZoomLevel(),
            shouldAnimateTransition = shouldAnimateCameraTransition,
        )
    }

    val minLatitude = points.minOf(MapCoordinate::latitude)
    val maxLatitude = points.maxOf(MapCoordinate::latitude)
    val minLongitude = points.minOf(MapCoordinate::longitude)
    val maxLongitude = points.maxOf(MapCoordinate::longitude)
    val latitudeSpan = (maxLatitude - minLatitude).coerceAtLeast(MIN_VIEWPORT_LATITUDE_SPAN)
    val longitudeSpan = (maxLongitude - minLongitude).coerceAtLeast(MIN_VIEWPORT_LONGITUDE_SPAN)

    return MapCameraTarget(
        center =
            MapCoordinate(
                latitude = (minLatitude + maxLatitude) / 2.0,
                longitude = (minLongitude + maxLongitude) / 2.0,
            ),
        source = MapCameraSource.SEARCH_RESULT,
        requestId = hashCode().toLong(),
        zoomLevel = approximateZoomLevel(latitudeSpan = latitudeSpan, longitudeSpan = longitudeSpan),
        shouldAnimateTransition = shouldAnimateCameraTransition,
    )
}

private fun MapViewportOverlayState.projectionCoordinates(): List<MapCoordinate> =
    buildList {
        polylines
            .filter(MapViewportPolylineOverlay::includeInProjection)
            .flatMapTo(this) { polyline -> polyline.points }
        points
            .filter(MapViewportPointOverlay::includeInProjection)
            .mapTo(this) { point -> point.coordinate }
    }.distinct()

private fun MapViewportFallbackCamera.toApproximateZoomLevel(): Int =
    approximateZoomLevel(
        latitudeSpan = latitudeSpan,
        longitudeSpan = longitudeSpan,
    )

private fun approximateZoomLevel(
    latitudeSpan: Double,
    longitudeSpan: Double,
): Int =
    when (max(latitudeSpan, longitudeSpan)) {
        in 0.0..0.0045 -> 18
        in 0.0045..0.0090 -> 17
        in 0.0090..0.0180 -> 16
        in 0.0180..0.0360 -> 15
        in 0.0360..0.0720 -> 14
        in 0.0720..0.1440 -> 13
        else -> 12
    }
