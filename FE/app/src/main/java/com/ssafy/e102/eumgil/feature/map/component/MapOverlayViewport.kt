package com.ssafy.e102.eumgil.feature.map.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ssafy.e102.eumgil.BuildConfig
import com.ssafy.e102.eumgil.feature.map.model.KAKAO_MAP_MAX_ZOOM_LEVEL
import com.ssafy.e102.eumgil.feature.map.model.KAKAO_MAP_MIN_ZOOM_LEVEL
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerLoadStatus
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerOverlayState
import com.ssafy.e102.eumgil.feature.map.model.resolvedZoomLevel
import kotlin.math.max

@Composable
internal fun MapOverlayViewport(
    overlayState: MapViewportOverlayState,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onMarkerClick: (String) -> Unit = {},
    controlState: MapOverlayViewportControlState? = null,
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
    val baseCameraTarget = overlayState.toMapCameraTarget()
    SideEffect {
        controlState?.updateBaseCameraTarget(baseCameraTarget)
    }
    val cameraTarget = controlState?.cameraTargetFor(baseCameraTarget) ?: baseCameraTarget
    val renderedOverlayState =
        if (controlState?.shouldFitProjection == false) {
            overlayState.copy(fitToProjection = false)
        } else {
            overlayState
        }

    when (integrationState) {
        is MapIntegrationState.Bound ->
            KakaoMapViewport(
                state =
                    MapViewportUiState(
                        integrationState = integrationState,
                        cameraTarget = cameraTarget,
                        currentLocation = null,
                        selectedDestinationCoordinate = null,
                        selectedDestinationName = null,
                        markerOverlayState = MapMarkerOverlayState(loadStatus = MapMarkerLoadStatus.READY),
                        overlayState = renderedOverlayState,
                        selectedMarkerId = null,
                        selectedMapPinCoordinate = null,
                        regionLabel = "",
                        statusLabel = "",
                        title = "",
                        description = "",
                        supportingText = "",
                    ),
                onMarkerClick = onMarkerClick,
                onCameraMoveEnd = { center, zoomLevel, isUserGesture, _ ->
                    controlState?.onCameraMoveEnd(
                        center = center,
                        zoomLevel = zoomLevel,
                        isUserGesture = isUserGesture,
                    )
                },
                onMapClick = {},
                modifier = describedModifier,
            )

        MapIntegrationState.Unbound ->
            MapViewportOverlayBackdrop(
                overlayState = renderedOverlayState,
                zoomLevel = cameraTarget.resolvedZoomLevel(),
                modifier = describedModifier,
                onPointClick = onMarkerClick,
            )
    }
}

@Composable
internal fun rememberMapOverlayViewportControlState(): MapOverlayViewportControlState =
    remember { MapOverlayViewportControlState() }

@Stable
internal class MapOverlayViewportControlState {
    private var baseCameraTarget: MapCameraTarget? by mutableStateOf(null)
    private var manualCameraTarget: MapCameraTarget? by mutableStateOf(null)
    private var latestObservedCamera: MapOverlayObservedCamera? by mutableStateOf(null)
    private var recenterCameraTarget: MapCameraTarget? by mutableStateOf(null)
    private var nextRequestId by mutableLongStateOf(MAP_OVERLAY_CONTROL_REQUEST_ID_START)

    val shouldFitProjection: Boolean
        get() = manualCameraTarget == null

    internal fun updateBaseCameraTarget(target: MapCameraTarget) {
        val previous = baseCameraTarget
        baseCameraTarget = target
        if (previous != null && previous.requestId != target.requestId) {
            manualCameraTarget = null
            latestObservedCamera = null
            recenterCameraTarget = null
        }
    }

    internal fun cameraTargetFor(baseTarget: MapCameraTarget): MapCameraTarget =
        manualCameraTarget ?: recenterCameraTarget ?: baseTarget

    internal fun onCameraMoveEnd(
        center: MapCoordinate,
        zoomLevel: Int,
        isUserGesture: Boolean,
    ) {
        latestObservedCamera =
            MapOverlayObservedCamera(
                center = center,
                zoomLevel = zoomLevel.coerceIn(KAKAO_MAP_MIN_ZOOM_LEVEL, KAKAO_MAP_MAX_ZOOM_LEVEL),
            )
        if (isUserGesture) {
            manualCameraTarget =
                (baseCameraTarget ?: MapCameraTarget.DefaultBusan).copy(
                    center = center,
                    zoomLevel = zoomLevel.coerceIn(KAKAO_MAP_MIN_ZOOM_LEVEL, KAKAO_MAP_MAX_ZOOM_LEVEL),
                    requestId = nextControlRequestId(),
                    shouldAnimateTransition = false,
                )
            recenterCameraTarget = null
        }
    }

    fun zoomIn() {
        zoomBy(1)
    }

    fun zoomOut() {
        zoomBy(-1)
    }

    fun recenter() {
        val baseTarget = baseCameraTarget ?: return
        manualCameraTarget = null
        latestObservedCamera = null
        recenterCameraTarget =
            baseTarget.copy(
                requestId = nextControlRequestId(),
                shouldAnimateTransition = true,
            )
    }

    private fun zoomBy(delta: Int) {
        val baseTarget = baseCameraTarget ?: MapCameraTarget.DefaultBusan
        val currentTarget = manualCameraTarget ?: baseTarget
        val observedCamera = latestObservedCamera
        val currentZoomLevel = observedCamera?.zoomLevel ?: currentTarget.resolvedZoomLevel()
        val nextZoomLevel =
            (currentZoomLevel + delta)
                .coerceIn(KAKAO_MAP_MIN_ZOOM_LEVEL, KAKAO_MAP_MAX_ZOOM_LEVEL)
        if (nextZoomLevel == currentZoomLevel) return
        manualCameraTarget =
            currentTarget.copy(
                center = observedCamera?.center ?: currentTarget.center,
                zoomLevel = nextZoomLevel,
                requestId = nextControlRequestId(),
                shouldAnimateTransition = true,
            )
        recenterCameraTarget = null
    }

    private fun nextControlRequestId(): Long {
        nextRequestId += 1
        return nextRequestId
    }
}

private data class MapOverlayObservedCamera(
    val center: MapCoordinate,
    val zoomLevel: Int,
)

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

private const val MAP_OVERLAY_CONTROL_REQUEST_ID_START = 1_000_000L
