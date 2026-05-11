package com.ssafy.e102.eumgil.feature.map.component

import androidx.annotation.DrawableRes
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerCategoryType
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

internal enum class KakaoProjectedMarkerKind {
    CURRENT_LOCATION,
    SELECTED_DESTINATION,
    SELECTED_MAP_PIN,
    ROUTE_ORIGIN,
    ROUTE_DESTINATION,
    ROUTE_SEGMENT_JUNCTION,
}

internal enum class KakaoOverlayMarkerKind {
    ROUTE_SEGMENT_JUNCTION,
}

internal data class KakaoProjectedMarkerRenderState(
    val markerId: String,
    val coordinate: MapCoordinate,
    val kind: KakaoProjectedMarkerKind,
    @DrawableRes val iconResId: Int,
    val anchorPointX: Float,
    val anchorPointY: Float,
    val sizeDp: Int,
    val zIndex: Float,
    val fillColorArgb: Int? = null,
    val strokeColorArgb: Int? = null,
)

internal data class KakaoOverlayMarkerRenderState(
    val markerId: String,
    val coordinate: MapCoordinate,
    val kind: KakaoOverlayMarkerKind,
    val anchorPointX: Float,
    val anchorPointY: Float,
    val sizeDp: Int,
    val zIndex: Float,
    val fillColorArgb: Int,
    val strokeColorArgb: Int,
)

internal data class KakaoProjectedMarkerOverlay(
    val markerId: String,
    val kind: KakaoProjectedMarkerKind,
    @DrawableRes val iconResId: Int,
    val screenPoint: KakaoMapScreenPoint,
    val anchorPointX: Float,
    val anchorPointY: Float,
    val sizeDp: Int,
    val zIndex: Float,
    val fillColorArgb: Int? = null,
    val strokeColorArgb: Int? = null,
)

internal data class KakaoProjectedMarkerProjectionResult(
    val overlays: List<KakaoProjectedMarkerOverlay>,
    val shouldRetry: Boolean,
)

internal data class KakaoRouteLineRenderState(
    val routeLineId: String,
    val points: List<MapCoordinate>,
    val lineWidth: Float,
    val lineColor: Int,
    val strokeWidth: Float,
    val strokeColor: Int,
    val zOrder: Int,
)

internal data class KakaoRouteCameraRenderState(
    val points: List<MapCoordinate>,
    val signature: Int,
)

internal data class KakaoMarkerRenderState(
    val markerId: String,
    val latitude: Double,
    val longitude: Double,
    val category: FacilityCategory,
    @DrawableRes val glyphResId: Int,
    val rank: Long,
    val clickTargetId: String?,
    val isSelected: Boolean,
    val sizeDp: Int,
    val anchorPointX: Float,
    val anchorPointY: Float,
)

internal data class KakaoRendererFailure(
    val reasonLabel: String,
    val detailMessage: String,
) {
    val debugSummary: String
        get() = "$reasonLabel: $detailMessage"
}

internal enum class KakaoRendererLoadingPhase {
    INITIALIZING,
    AUTOMATIC_RETRY,
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

internal fun shouldAutoRestartKakaoRenderer(
    failure: KakaoRendererFailure,
    attemptedAutomaticRecoveryCount: Int,
): Boolean =
    attemptedAutomaticRecoveryCount < 1 &&
        (
            failure.reasonLabel == KAKAO_RENDERER_TIMEOUT_REASON_LABEL ||
                failure.reasonLabel == KAKAO_RENDERER_DESTROYED_REASON_LABEL
        )

internal fun resolveKakaoRendererLoadingPhase(
    attemptedAutomaticRecoveryCount: Int,
): KakaoRendererLoadingPhase =
    if (attemptedAutomaticRecoveryCount > 0) {
        KakaoRendererLoadingPhase.AUTOMATIC_RETRY
    } else {
        KakaoRendererLoadingPhase.INITIALIZING
    }

internal fun resolveKakaoRendererFailureAfterUnexpectedDestroy(
    existingFailure: KakaoRendererFailure?,
): KakaoRendererFailure = existingFailure ?: createKakaoRendererDestroyedFailure()

internal fun createKakaoRendererDestroyedFailure(): KakaoRendererFailure =
    KakaoRendererFailure(
        reasonLabel = KAKAO_RENDERER_DESTROYED_REASON_LABEL,
        detailMessage = KAKAO_RENDERER_DESTROYED_DETAIL_FALLBACK,
    )

internal fun createKakaoMarkerRenderStates(
    markerOverlayState: MapMarkerOverlayState,
    selectedMarkerId: String?,
): List<KakaoMarkerRenderState> =
    buildList {
        addAll(
            markerOverlayState.visibleMarkers.map { marker ->
                val isSelected = marker.markerId == selectedMarkerId
                KakaoMarkerRenderState(
                    markerId = marker.markerId,
                    latitude = marker.coordinate.latitude,
                    longitude = marker.coordinate.longitude,
                    category = marker.categoryType.category,
                    glyphResId = facilityMarkerGlyphResId(marker.categoryType.category),
                    rank = if (isSelected) KAKAO_SELECTED_MARKER_RANK else KAKAO_DEFAULT_MARKER_RANK,
                    clickTargetId = marker.markerId,
                    isSelected = isSelected,
                    sizeDp = resolveKakaoFacilityMarkerSizeDp(marker.categoryType.category, isSelected),
                    anchorPointX = KAKAO_FACILITY_MARKER_ANCHOR_POINT_X,
                    anchorPointY = KAKAO_FACILITY_MARKER_ANCHOR_POINT_Y,
                )
            },
        )
    }

internal fun createKakaoProjectedMarkerRenderStates(
    currentLocation: MapCoordinate?,
    selectedDestinationCoordinate: MapCoordinate?,
    selectedMapPinCoordinate: MapCoordinate?,
    overlayPoints: List<MapViewportPointOverlay> = emptyList(),
): List<KakaoProjectedMarkerRenderState> {
    val projectedMarkers =
        buildList {
        currentLocation?.let { coordinate ->
            add(
                KakaoProjectedMarkerRenderState(
                    markerId = "current-location",
                    coordinate = coordinate,
                    kind = KakaoProjectedMarkerKind.CURRENT_LOCATION,
                    iconResId = R.drawable.ic_map_current_location,
                    anchorPointX = 0.5f,
                    anchorPointY = 0.5f,
                    sizeDp = 28,
                    zIndex = 2f,
                ),
            )
        }
        addAll(
            overlayPoints.mapNotNull { point ->
                point.toProjectedMarkerRenderState(
                    includeCurrentLocation = currentLocation == null,
                )
            },
        )
        if (selectedMapPinCoordinate == null) {
            selectedDestinationCoordinate?.let { coordinate ->
                add(
                    KakaoProjectedMarkerRenderState(
                        markerId = "selected-destination",
                        coordinate = coordinate,
                        kind = KakaoProjectedMarkerKind.SELECTED_DESTINATION,
                        iconResId = R.drawable.ic_map_selected_pin_blue,
                        anchorPointX = 0.5f,
                        anchorPointY = 1.0f,
                        sizeDp = 32,
                        zIndex = 3f,
                    ),
                )
            }
        }
        selectedMapPinCoordinate?.let { coordinate ->
            add(
                KakaoProjectedMarkerRenderState(
                    markerId = "selected-map-pin",
                    coordinate = coordinate,
                    kind = KakaoProjectedMarkerKind.SELECTED_MAP_PIN,
                    iconResId = R.drawable.ic_map_selected_pin_blue,
                    anchorPointX = 0.5f,
                    anchorPointY = 1.0f,
                    sizeDp = 32,
                    zIndex = 4f,
                ),
            )
        }
    }
    logProjectedSegmentMarkerDebugSummary(projectedMarkers)
    return projectedMarkers
}

internal fun createKakaoOverlayMarkerRenderStates(
    overlayPoints: List<MapViewportPointOverlay>,
): List<KakaoOverlayMarkerRenderState> =
    overlayPoints.mapNotNull(MapViewportPointOverlay::toOverlayMarkerRenderState)

internal fun createKakaoRouteLineRenderStates(
    polylines: List<MapViewportPolylineOverlay>,
): List<KakaoRouteLineRenderState> =
    polylines
        .filter(MapViewportPolylineOverlay::isRenderable)
        .mapIndexed { index, polyline ->
            val style = polyline.toKakaoRouteLineStyle()
            KakaoRouteLineRenderState(
                routeLineId = polyline.overlayId,
                points = polyline.points,
                lineWidth = style.lineWidth,
                lineColor = style.lineColor,
                strokeWidth = style.strokeWidth,
                strokeColor = style.strokeColor,
                zOrder = KAKAO_ROUTE_LINE_BASE_Z_ORDER + index,
            )
        }

internal fun createKakaoRouteCameraRenderState(
    overlayState: MapViewportOverlayState,
): KakaoRouteCameraRenderState? {
    val projectionPoints =
        buildList {
            overlayState.polylines
                .filter(MapViewportPolylineOverlay::includeInProjection)
                .flatMapTo(this) { polyline -> polyline.points }
            overlayState.points
                .filter(MapViewportPointOverlay::includeInProjection)
                .mapTo(this) { point -> point.coordinate }
        }.distinct()

    if (projectionPoints.size < 2) return null

    return KakaoRouteCameraRenderState(
        points = projectionPoints,
        signature = projectionPoints.hashCode(),
    )
}

internal fun createKakaoProjectedMarkerOverlays(
    projectedMarkers: List<KakaoProjectedMarkerRenderState>,
    projectScreenPoint: (MapCoordinate) -> KakaoMapScreenPoint?,
): List<KakaoProjectedMarkerOverlay> =
    projectedMarkers.mapNotNull { marker ->
        projectScreenPoint(marker.coordinate)?.let { screenPoint ->
            KakaoProjectedMarkerOverlay(
                markerId = marker.markerId,
                kind = marker.kind,
                iconResId = marker.iconResId,
                screenPoint = screenPoint,
                anchorPointX = marker.anchorPointX,
                anchorPointY = marker.anchorPointY,
                sizeDp = marker.sizeDp,
                zIndex = marker.zIndex,
                fillColorArgb = marker.fillColorArgb,
                strokeColorArgb = marker.strokeColorArgb,
            )
        }
    }

internal fun createKakaoProjectedMarkerProjectionResult(
    projectedMarkers: List<KakaoProjectedMarkerRenderState>,
    projectScreenPoint: (MapCoordinate) -> KakaoMapScreenPoint?,
): KakaoProjectedMarkerProjectionResult {
    val overlays = createKakaoProjectedMarkerOverlays(projectedMarkers, projectScreenPoint)
    return KakaoProjectedMarkerProjectionResult(
        overlays = overlays,
        shouldRetry = projectedMarkers.isNotEmpty() && overlays.isEmpty(),
    )
}

internal fun createProjectedSegmentMarkerDebugSummary(
    projectedMarkers: List<KakaoProjectedMarkerRenderState>,
): String {
    val segmentMarkers =
        projectedMarkers.filter { marker ->
            marker.kind == KakaoProjectedMarkerKind.ROUTE_SEGMENT_JUNCTION
        }
    return buildString {
        append("total=")
        append(projectedMarkers.size)
        append(" segmentProjected=")
        append(segmentMarkers.size)
        append(" details=[")
        append(
            segmentMarkers.joinToString(separator = "; ") { marker ->
                buildString {
                    append("id=")
                    append(marker.markerId)
                    append(" coord=")
                    append(marker.coordinate.toDebugCoordinate())
                    append(" sizeDp=")
                    append(marker.sizeDp)
                    append(" z=")
                    append(marker.zIndex)
                    append(" fill=")
                    append(marker.fillColorArgb.toDebugColor())
                    append(" stroke=")
                    append(marker.strokeColorArgb.toDebugColor())
                }
            },
        )
        append("]")
    }
}

internal fun createProjectedSegmentMarkerPipelineDebugSummary(
    projectedMarkers: List<KakaoProjectedMarkerRenderState>,
    projectionResult: KakaoProjectedMarkerProjectionResult,
    isCameraMoveInProgress: Boolean,
    retryScheduled: Boolean,
    retryCount: Int,
): String {
    val segmentProjectedCount =
        projectedMarkers.count { marker ->
            marker.kind == KakaoProjectedMarkerKind.ROUTE_SEGMENT_JUNCTION
        }
    val segmentRendered =
        projectionResult.overlays.filter { overlay ->
            overlay.kind == KakaoProjectedMarkerKind.ROUTE_SEGMENT_JUNCTION
        }
    return buildString {
        append("projected=")
        append(projectedMarkers.size)
        append(" segmentProjected=")
        append(segmentProjectedCount)
        append(" overlays=")
        append(projectionResult.overlays.size)
        append(" segmentRendered=")
        append(segmentRendered.size)
        append(" shouldRetry=")
        append(projectionResult.shouldRetry)
        append(" retryScheduled=")
        append(retryScheduled)
        append(" retryCount=")
        append(retryCount)
        append(" cameraMoving=")
        append(isCameraMoveInProgress)
        append(" details=[")
        append(
            segmentRendered.joinToString(separator = "; ") { overlay ->
                buildString {
                    append("id=")
                    append(overlay.markerId)
                    append(" screen=")
                    append(overlay.screenPoint.x)
                    append(",")
                    append(overlay.screenPoint.y)
                    append(" sizeDp=")
                    append(overlay.sizeDp)
                    append(" z=")
                    append(overlay.zIndex)
                    append(" fill=")
                    append(overlay.fillColorArgb.toDebugColor())
                    append(" stroke=")
                    append(overlay.strokeColorArgb.toDebugColor())
                }
            },
        )
        append("]")
    }
}

internal fun createRenderedSegmentJunctionOverlayDebugSummary(
    overlays: List<KakaoProjectedMarkerOverlay>,
): String {
    val segmentRendered =
        overlays.filter { overlay ->
            overlay.kind == KakaoProjectedMarkerKind.ROUTE_SEGMENT_JUNCTION
        }
    return buildString {
        append("count=")
        append(segmentRendered.size)
        append(" details=[")
        append(
            segmentRendered.joinToString(separator = "; ") { overlay ->
                buildString {
                    append("id=")
                    append(overlay.markerId)
                    append(" screen=")
                    append(overlay.screenPoint.x)
                    append(",")
                    append(overlay.screenPoint.y)
                    append(" sizeDp=")
                    append(overlay.sizeDp)
                    append(" z=")
                    append(overlay.zIndex)
                    append(" fill=")
                    append(overlay.fillColorArgb.toDebugColor())
                    append(" stroke=")
                    append(overlay.strokeColorArgb.toDebugColor())
                }
            },
        )
        append("]")
    }
}

internal fun createProjectedSegmentRenderPathDebugSummary(
    overlays: List<KakaoProjectedMarkerOverlay>,
): String {
    val segmentRendered =
        overlays.filter { overlay ->
            overlay.kind == KakaoProjectedMarkerKind.ROUTE_SEGMENT_JUNCTION
        }
    return buildString {
        append("renderPath=projected")
        append(" overlayCount=")
        append(overlays.size)
        append(" segmentCount=")
        append(segmentRendered.size)
        append(" details=[")
        append(
            segmentRendered.joinToString(separator = "; ") { overlay ->
                buildString {
                    append("id=")
                    append(overlay.markerId)
                    append(" screen=")
                    append(overlay.screenPoint.x)
                    append(",")
                    append(overlay.screenPoint.y)
                    append(" sizeDp=")
                    append(overlay.sizeDp)
                    append(" z=")
                    append(overlay.zIndex)
                    append(" fill=")
                    append(overlay.fillColorArgb.toDebugColor())
                    append(" stroke=")
                    append(overlay.strokeColorArgb.toDebugColor())
                }
            },
        )
        append("]")
    }
}

internal fun createNativeSegmentRenderPathDebugSummary(
    layerId: String,
    markers: List<KakaoOverlayMarkerRenderState>,
): String {
    val segmentMarkers =
        markers.filter { marker ->
            marker.kind == KakaoOverlayMarkerKind.ROUTE_SEGMENT_JUNCTION
        }
    return buildString {
        append("renderPath=native-label")
        append(" layer=")
        append(layerId)
        append(" markerCount=")
        append(markers.size)
        append(" segmentCount=")
        append(segmentMarkers.size)
        append(" details=[")
        append(
            segmentMarkers.joinToString(separator = "; ") { marker ->
                buildString {
                    append("id=")
                    append(marker.markerId)
                    append(" coord=")
                    append(marker.coordinate.toDebugCoordinate())
                    append(" sizeDp=")
                    append(marker.sizeDp)
                    append(" z=")
                    append(marker.zIndex)
                    append(" fill=")
                    append(marker.fillColorArgb.toDebugColor())
                    append(" stroke=")
                    append(marker.strokeColorArgb.toDebugColor())
                }
            },
        )
        append("]")
    }
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

private data class KakaoRouteLineStyleSpec(
    val lineWidth: Float,
    val lineColor: Int,
    val strokeWidth: Float,
    val strokeColor: Int,
)

private data class KakaoRouteLinePalette(
    val lineColor: Int,
    val casingColor: Int,
)

private data class KakaoOverlayPointMarkerSpec(
    val kind: KakaoProjectedMarkerKind,
    @DrawableRes val iconResId: Int,
    val sizeDp: Int,
    val anchorPointY: Float,
    val zIndex: Float,
    val fillColorArgb: Int? = null,
    val strokeColorArgb: Int? = null,
)

private fun MapViewportPointOverlay.toProjectedMarkerRenderState(
    includeCurrentLocation: Boolean,
): KakaoProjectedMarkerRenderState? {
    val markerSpec =
        when (kind) {
            MapViewportPointKind.ORIGIN ->
                KakaoOverlayPointMarkerSpec(
                    kind = KakaoProjectedMarkerKind.ROUTE_ORIGIN,
                    iconResId = R.drawable.ic_navigation_rail_origin_pin,
                    sizeDp = 34,
                    anchorPointY = 1.0f,
                    zIndex = 4f,
                )

            MapViewportPointKind.DESTINATION ->
                KakaoOverlayPointMarkerSpec(
                    kind = KakaoProjectedMarkerKind.ROUTE_DESTINATION,
                    iconResId = R.drawable.ic_navigation_rail_destination_pin,
                    sizeDp = 34,
                    anchorPointY = 1.0f,
                    zIndex = 5f,
                )

            MapViewportPointKind.CURRENT_LOCATION ->
                if (includeCurrentLocation) {
                    KakaoOverlayPointMarkerSpec(
                        kind = KakaoProjectedMarkerKind.CURRENT_LOCATION,
                        iconResId = R.drawable.ic_map_current_location,
                        sizeDp = 28,
                        anchorPointY = 0.5f,
                        zIndex = 3f,
                    )
                } else {
                    null
                }

            MapViewportPointKind.SEGMENT_JUNCTION -> null

            MapViewportPointKind.FACILITY,
            MapViewportPointKind.CAMERA_FOCUS,
            MapViewportPointKind.FOCUS_HALO,
                -> null
        } ?: return null

    return KakaoProjectedMarkerRenderState(
        markerId = "overlay-$overlayId",
        coordinate = coordinate,
        kind = markerSpec.kind,
        iconResId = markerSpec.iconResId,
        anchorPointX = 0.5f,
        anchorPointY = markerSpec.anchorPointY,
        sizeDp = markerSpec.sizeDp,
        zIndex = markerSpec.zIndex,
        fillColorArgb = markerSpec.fillColorArgb,
        strokeColorArgb = markerSpec.strokeColorArgb,
    )
}

private fun MapViewportPointOverlay.toOverlayMarkerRenderState(): KakaoOverlayMarkerRenderState? {
    if (kind != MapViewportPointKind.SEGMENT_JUNCTION) return null
    val palette = (tone ?: MapViewportOverlayTone.PRIMARY).toSegmentMarkerPalette()
    return KakaoOverlayMarkerRenderState(
        markerId = "overlay-$overlayId",
        coordinate = coordinate,
        kind = KakaoOverlayMarkerKind.ROUTE_SEGMENT_JUNCTION,
        anchorPointX = 0.5f,
        anchorPointY = 0.5f,
        sizeDp = 16,
        zIndex = 3.6f,
        fillColorArgb = palette.fillColorArgb,
        strokeColorArgb = palette.strokeColorArgb,
    )
}

private fun MapViewportPolylineOverlay.toKakaoRouteLineStyle(): KakaoRouteLineStyleSpec {
    val palette = tone.toKakaoRouteLinePalette()
    return when (style) {
        MapViewportPolylineStyle.ROUTE_PREVIEW ->
            KakaoRouteLineStyleSpec(
                lineWidth = 5f,
                lineColor = palette.lineColor,
                strokeWidth = 6.5f,
                strokeColor = palette.casingColor,
            )

        MapViewportPolylineStyle.ROUTE_BASELINE ->
            KakaoRouteLineStyleSpec(
                lineWidth = 3.5f,
                lineColor = palette.lineColor,
                strokeWidth = 5f,
                strokeColor = palette.casingColor,
            )

        MapViewportPolylineStyle.ACTIVE_SEGMENT ->
            KakaoRouteLineStyleSpec(
                lineWidth = 4f,
                lineColor = palette.lineColor,
                strokeWidth = 5.5f,
                strokeColor = palette.casingColor,
            )

        MapViewportPolylineStyle.FOCUSED_SEGMENT ->
            KakaoRouteLineStyleSpec(
                lineWidth = 4.5f,
                lineColor = palette.lineColor,
                strokeWidth = 6f,
                strokeColor = palette.casingColor,
            )
    }
}

private fun MapViewportOverlayTone.toKakaoRouteLinePalette(): KakaoRouteLinePalette =
    when (this) {
        MapViewportOverlayTone.PRIMARY ->
            KakaoRouteLinePalette(
                lineColor = 0xFF2A7BFF.toInt(),
                casingColor = 0xFF0F4FC6.toInt(),
            )

        MapViewportOverlayTone.SECONDARY ->
            KakaoRouteLinePalette(
                lineColor = 0xFF14AA82.toInt(),
                casingColor = 0xFF0A7B5E.toInt(),
            )

        MapViewportOverlayTone.TERTIARY ->
            KakaoRouteLinePalette(
                lineColor = 0xFFE7832F.toInt(),
                casingColor = 0xFFB85B16.toInt(),
            )

        MapViewportOverlayTone.ERROR ->
            KakaoRouteLinePalette(
                lineColor = 0xFFD94C4C.toInt(),
                casingColor = 0xFF9D2A2A.toInt(),
            )
    }

// Kakao labels render raw drawable bounds, so map markers must use compact icon assets.
@DrawableRes
internal fun facilityMarkerGlyphResId(category: FacilityCategory): Int =
    when (category) {
        FacilityCategory.TOILET -> R.drawable.ic_place_restroom
        FacilityCategory.ELEVATOR -> R.drawable.ic_lowvision_category_elevator
        FacilityCategory.CHARGING_STATION -> R.drawable.ic_place_charging
        FacilityCategory.FOOD_CAFE -> R.drawable.ic_place_cafe
        FacilityCategory.TOURIST_SPOT -> R.drawable.ic_nav_facility
        FacilityCategory.ACCOMMODATION -> R.drawable.ic_place_accommodation
        FacilityCategory.HEALTHCARE -> R.drawable.ic_place_healthcare
        FacilityCategory.WELFARE -> R.drawable.ic_place_welfare
        FacilityCategory.PUBLIC_OFFICE -> R.drawable.ic_place_public_office
        FacilityCategory.BRAILLE_BLOCK -> R.drawable.ic_route_tactile_blocks
        FacilityCategory.RESTAURANT -> R.drawable.ic_place_restaurant
        FacilityCategory.TOURIST_ATTRACTION -> R.drawable.ic_nav_facility
        FacilityCategory.OTHER -> R.drawable.ic_nav_facility
    }

internal fun resolveKakaoFacilityMarkerSizeDp(
    category: FacilityCategory,
    isSelected: Boolean,
): Int =
    when {
        isSelected -> 34
        category == FacilityCategory.BRAILLE_BLOCK -> 30
        else -> 28
    }

private const val KAKAO_DEFAULT_MARKER_RANK = 0L
private const val KAKAO_SELECTED_MARKER_RANK = 10L
private const val KAKAO_FACILITY_MARKER_ANCHOR_POINT_X = 0.5f
private const val KAKAO_FACILITY_MARKER_ANCHOR_POINT_Y = 0.5f
private const val KAKAO_ROUTE_LINE_BASE_Z_ORDER = 0

internal const val KAKAO_RENDERER_ERROR_REASON_FALLBACK = "MapError"
internal const val KAKAO_RENDERER_ERROR_DETAIL_FALLBACK = "Unknown renderer failure"
internal const val KAKAO_RENDERER_DESTROYED_REASON_LABEL = "MapDestroyed"
internal const val KAKAO_RENDERER_DESTROYED_DETAIL_FALLBACK = "Renderer was destroyed before becoming ready"
internal const val KAKAO_RENDERER_TIMEOUT_REASON_LABEL = "MapTimeout"
internal const val KAKAO_RENDERER_TIMEOUT_DETAIL_FALLBACK = "Renderer did not become ready in time"
internal const val KAKAO_ZOOM_CAMERA_ANIMATION_DURATION_MILLIS = 220

private fun Double.toLogCoordinate(): String = String.format(Locale.US, "%.6f", this)

private var lastProjectedSegmentMarkerDebugSummary: String? = null

private fun logProjectedSegmentMarkerDebugSummary(
    projectedMarkers: List<KakaoProjectedMarkerRenderState>,
) {
    val summary = createProjectedSegmentMarkerDebugSummary(projectedMarkers)
    if (summary == lastProjectedSegmentMarkerDebugSummary) return
    lastProjectedSegmentMarkerDebugSummary = summary
    println("SegmentMarkerTrace[KakaoProjectedMarkers] $summary")
}

private fun MapCoordinate.toDebugCoordinate(): String =
    String.format(Locale.US, "%.6f,%.6f", latitude, longitude)

private fun Int?.toDebugColor(): String =
    this?.let { color ->
        String.format(Locale.US, "0x%08X", color)
    } ?: "null"
