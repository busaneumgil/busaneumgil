package com.ssafy.e102.eumgil.feature.map.component

import androidx.annotation.DrawableRes
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerOverlayState
import com.ssafy.e102.eumgil.feature.map.model.resolvedZoomLevel
import java.util.Locale

internal const val KAKAO_MAP_PROVIDER_NAME = "Kakao Map"

internal fun resolveMapIntegrationState(
    hasNativeAppKey: Boolean,
    isInspectionMode: Boolean,
): MapIntegrationState =
    if (hasNativeAppKey && !isInspectionMode) {
        MapIntegrationState.Bound(providerName = KAKAO_MAP_PROVIDER_NAME)
    } else {
        MapIntegrationState.Unbound
    }

internal data class KakaoCameraRenderState(
    val latitude: Double,
    val longitude: Double,
    val zoomLevel: Int,
    val requestId: Long,
)

internal fun createKakaoCameraRenderState(cameraTarget: MapCameraTarget): KakaoCameraRenderState =
    KakaoCameraRenderState(
        latitude = cameraTarget.center.latitude,
        longitude = cameraTarget.center.longitude,
        zoomLevel = cameraTarget.resolvedZoomLevel(),
        requestId = cameraTarget.requestId,
    )

internal fun createKakaoCameraDebugSummary(cameraTarget: MapCameraTarget): String {
    val cameraState = createKakaoCameraRenderState(cameraTarget)
    return buildString {
        append("requestId=")
        append(cameraState.requestId)
        append(" source=")
        append(cameraTarget.source.name)
        append(" lat=")
        append(cameraState.latitude.toLogCoordinate())
        append(" lng=")
        append(cameraState.longitude.toLogCoordinate())
        append(" zoom=")
        append(cameraState.zoomLevel)
    }
}

internal fun shouldAnimateKakaoCameraTransition(
    previousTarget: MapCameraTarget?,
    nextTarget: MapCameraTarget,
): Boolean {
    if (previousTarget == null) return false
    if (previousTarget.requestId == nextTarget.requestId) return false
    if (previousTarget.source != nextTarget.source) return false
    if (previousTarget.center != nextTarget.center) return false
    return previousTarget.resolvedZoomLevel() != nextTarget.resolvedZoomLevel()
}

internal fun syncRenderedKakaoCameraTarget(
    previousTarget: MapCameraTarget?,
    latestStateTarget: MapCameraTarget?,
    center: MapCoordinate,
    zoomLevel: Int,
): MapCameraTarget {
    val baseTarget = latestStateTarget ?: previousTarget ?: MapCameraTarget.DefaultBusan
    return baseTarget.copy(
        center = center,
        zoomLevel = zoomLevel,
    )
}

internal data class KakaoMapScreenPoint(
    val x: Int,
    val y: Int,
)

internal fun resolveSelectedMapPinScreenPoint(
    projectedScreenPoint: KakaoMapScreenPoint?,
    fallbackScreenPoint: KakaoMapScreenPoint?,
): KakaoMapScreenPoint? = projectedScreenPoint ?: fallbackScreenPoint

internal data class KakaoMarkerRenderState(
    val markerId: String,
    val latitude: Double,
    val longitude: Double,
    @DrawableRes val iconResId: Int,
    val rank: Long,
    val clickTargetId: String?,
)

internal data class KakaoRendererFailure(
    val reasonLabel: String,
    val detailMessage: String,
) {
    val debugSummary: String
        get() = "$reasonLabel: $detailMessage"
}

internal enum class KakaoMapLifecycleCommand {
    NONE,
    RESUME,
    PAUSE,
}

internal fun resolveKakaoMapLifecycleCommand(
    isLifecycleResumed: Boolean,
    hasMapView: Boolean,
    isAttachedToWindow: Boolean,
    isStarted: Boolean,
    isFinished: Boolean,
    hasResumedLifecycle: Boolean,
): KakaoMapLifecycleCommand =
    when {
        !hasMapView -> KakaoMapLifecycleCommand.NONE
        !isAttachedToWindow -> KakaoMapLifecycleCommand.NONE
        !isStarted -> KakaoMapLifecycleCommand.NONE
        isFinished -> KakaoMapLifecycleCommand.NONE
        // MapView.resume() drives the renderer lifecycle; waiting for surface creation deadlocks startup.
        isLifecycleResumed && hasResumedLifecycle -> KakaoMapLifecycleCommand.NONE
        isLifecycleResumed -> KakaoMapLifecycleCommand.RESUME
        !hasResumedLifecycle -> KakaoMapLifecycleCommand.NONE
        else -> KakaoMapLifecycleCommand.PAUSE
    }

internal fun createKakaoRendererFailure(error: Throwable): KakaoRendererFailure {
    val reasonLabel = error::class.simpleName ?: KAKAO_RENDERER_ERROR_REASON_FALLBACK
    val detailMessage =
        error.message
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: KAKAO_RENDERER_ERROR_DETAIL_FALLBACK
    return KakaoRendererFailure(
        reasonLabel = reasonLabel,
        detailMessage = detailMessage,
    )
}

internal fun createKakaoRendererTimeoutFailure(): KakaoRendererFailure =
    KakaoRendererFailure(
        reasonLabel = KAKAO_RENDERER_TIMEOUT_REASON_LABEL,
        detailMessage = KAKAO_RENDERER_TIMEOUT_DETAIL_FALLBACK,
    )

internal fun createKakaoMarkerRenderStates(
    markerOverlayState: MapMarkerOverlayState,
    selectedMarkerId: String?,
    currentLocation: com.ssafy.e102.eumgil.feature.map.model.MapCoordinate?,
    selectedMapPinCoordinate: com.ssafy.e102.eumgil.feature.map.model.MapCoordinate? = null,
): List<KakaoMarkerRenderState> =
    buildList {
        selectedMapPinCoordinate?.let { coordinate ->
            add(
                KakaoMarkerRenderState(
                    markerId = "selected-map-pin",
                    latitude = coordinate.latitude,
                    longitude = coordinate.longitude,
                    iconResId = R.drawable.ic_map_selected_pin_blue,
                    rank = 2L,
                    clickTargetId = null,
                ),
            )
        }
        currentLocation?.let { coordinate ->
            add(
                KakaoMarkerRenderState(
                    markerId = "current-location",
                    latitude = coordinate.latitude,
                    longitude = coordinate.longitude,
                    iconResId = R.drawable.ic_map_current_location,
                    rank = 0L,
                    clickTargetId = null,
                ),
            )
        }
        addAll(
            markerOverlayState.visibleMarkers.map { marker ->
                KakaoMarkerRenderState(
                    markerId = marker.markerId,
                    latitude = marker.coordinate.latitude,
                    longitude = marker.coordinate.longitude,
                    iconResId = categoryMarkerIconResId(marker.categoryType.category),
                    rank = if (marker.markerId == selectedMarkerId) 1L else 0L,
                    clickTargetId = marker.markerId,
                )
            },
        )
    }

internal fun createKakaoMarkerDebugSummary(
    markerOverlayState: MapMarkerOverlayState,
    renderedMarkers: List<KakaoMarkerRenderState>,
    selectedMarkerId: String?,
): String =
    buildString {
        append("total=")
        append(markerOverlayState.totalMarkerCount)
        append(" visible=")
        append(markerOverlayState.visibleMarkerCount)
        append(" rendered=")
        append(renderedMarkers.size)
        append(" selected=")
        append(selectedMarkerId ?: "none")
    }

// Kakao labels render raw drawable bounds, so map markers must use compact icon assets.
@DrawableRes
private fun categoryMarkerIconResId(category: FacilityCategory): Int =
    when (category) {
        FacilityCategory.TOILET -> R.drawable.ic_place_restroom
        FacilityCategory.ELEVATOR -> R.drawable.ic_lowvision_category_elevator
        FacilityCategory.CHARGING_STATION -> R.drawable.ic_place_charging
        FacilityCategory.FOOD_CAFE -> R.drawable.ic_place_cafe
        FacilityCategory.TOURIST_SPOT -> R.drawable.ic_nav_facility
        FacilityCategory.ACCOMMODATION -> R.drawable.ic_nav_facility
        FacilityCategory.HEALTHCARE -> R.drawable.ic_place_hospital
        FacilityCategory.WELFARE -> R.drawable.ic_nav_facility
        FacilityCategory.PUBLIC_OFFICE -> R.drawable.ic_nav_facility
        FacilityCategory.BRAILLE_BLOCK -> R.drawable.ic_route_tactile_blocks
        FacilityCategory.RESTAURANT -> R.drawable.ic_place_restaurant
        FacilityCategory.TOURIST_ATTRACTION -> R.drawable.ic_nav_facility
        FacilityCategory.OTHER -> R.drawable.ic_nav_facility
    }

internal const val KAKAO_RENDERER_ERROR_REASON_FALLBACK = "MapError"
internal const val KAKAO_RENDERER_ERROR_DETAIL_FALLBACK = "Unknown renderer failure"
internal const val KAKAO_RENDERER_TIMEOUT_REASON_LABEL = "MapTimeout"
internal const val KAKAO_RENDERER_TIMEOUT_DETAIL_FALLBACK = "Renderer did not become ready in time"
internal const val KAKAO_ZOOM_CAMERA_ANIMATION_DURATION_MILLIS = 220

private fun Double.toLogCoordinate(): String = String.format(Locale.US, "%.6f", this)
