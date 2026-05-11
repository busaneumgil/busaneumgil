package com.ssafy.e102.eumgil.feature.map.component

import androidx.compose.runtime.Immutable
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerCategoryType
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerOverlayState
import com.ssafy.e102.eumgil.feature.navigation.NavigationMapFocusMode
import com.ssafy.e102.eumgil.feature.navigation.NavigationMapOverlayUiState
import com.ssafy.e102.eumgil.feature.navigation.NavigationMapSegmentUiState
import com.ssafy.e102.eumgil.feature.navigation.NavigationSegmentTravelKind
import com.ssafy.e102.eumgil.feature.route.RoutePreviewMapUiState

@Immutable
internal data class MapViewportOverlayState(
    val fallbackCamera: MapViewportFallbackCamera = defaultMapViewportFallbackCamera(),
    val points: List<MapViewportPointOverlay> = emptyList(),
    val polylines: List<MapViewportPolylineOverlay> = emptyList(),
)

@Immutable
internal data class MapViewportFallbackCamera(
    val center: MapCoordinate,
    val latitudeSpan: Double,
    val longitudeSpan: Double,
)

@Immutable
internal data class MapViewportPointOverlay(
    val overlayId: String,
    val coordinate: MapCoordinate,
    val kind: MapViewportPointKind,
    val tone: MapViewportOverlayTone? = null,
    val categoryType: MapMarkerCategoryType? = null,
    val label: String? = null,
    val contentDescription: String? = null,
    val isSelected: Boolean = false,
    val includeInProjection: Boolean = true,
    val clickTargetId: String? = null,
)

internal enum class MapViewportPointKind {
    FACILITY,
    ORIGIN,
    DESTINATION,
    CURRENT_LOCATION,
    SEGMENT_JUNCTION,
    CAMERA_FOCUS,
    FOCUS_HALO,
}

@Immutable
internal data class MapViewportPolylineOverlay(
    val overlayId: String,
    val points: List<MapCoordinate>,
    val style: MapViewportPolylineStyle,
    val tone: MapViewportOverlayTone,
    val includeInProjection: Boolean = true,
) {
    val isRenderable: Boolean
        get() = points.size >= 2
}

internal enum class MapViewportPolylineStyle {
    ROUTE_PREVIEW,
    ROUTE_BASELINE,
    ACTIVE_SEGMENT,
    FOCUSED_SEGMENT,
}

internal enum class MapViewportOverlayTone {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    ERROR,
}

@Immutable
internal data class MapViewportSegmentMarkerPalette(
    val fillColorArgb: Int,
    val strokeColorArgb: Int,
)

internal fun createMapMarkerViewportOverlayState(
    cameraTarget: MapCameraTarget,
    markerOverlayState: MapMarkerOverlayState,
    selectedMarkerId: String?,
    currentLocation: MapCoordinate?,
    currentLocationLabel: String?,
): MapViewportOverlayState =
    MapViewportOverlayState(
        fallbackCamera = cameraTarget.toViewportFallbackCamera(),
        points =
            buildList {
                add(
                    MapViewportPointOverlay(
                        overlayId = "camera-focus",
                        coordinate = cameraTarget.center,
                        kind = MapViewportPointKind.CAMERA_FOCUS,
                        includeInProjection = false,
                    ),
                )
                currentLocation?.let { coordinate ->
                    add(
                        MapViewportPointOverlay(
                            overlayId = "current-location",
                            coordinate = coordinate,
                            kind = MapViewportPointKind.CURRENT_LOCATION,
                            label = currentLocationLabel,
                            contentDescription = currentLocationLabel,
                        ),
                    )
                }
                addAll(
                    markerOverlayState.visibleMarkers.map { marker ->
                        MapViewportPointOverlay(
                            overlayId = marker.markerId,
                            coordinate = marker.coordinate,
                            kind = MapViewportPointKind.FACILITY,
                            categoryType = marker.categoryType,
                            contentDescription = marker.name,
                            isSelected = marker.markerId == selectedMarkerId,
                            clickTargetId = marker.markerId,
                        )
                    },
                )
            },
    )

internal fun createRoutePreviewViewportOverlayState(
    previewMap: RoutePreviewMapUiState,
    routeTone: MapViewportOverlayTone = previewMap.routeOption.toViewportOverlayTone(),
): MapViewportOverlayState =
    MapViewportOverlayState(
        points =
            listOfNotNull(
                previewMap.originCoordinate?.toOverlayPoint(
                    overlayId = "route-origin",
                    kind = MapViewportPointKind.ORIGIN,
                    label = "O",
                ),
                previewMap.destinationCoordinate?.toOverlayPoint(
                    overlayId = "route-destination",
                    kind = MapViewportPointKind.DESTINATION,
                    label = "D",
                ),
            ),
        polylines =
            listOf(
                MapViewportPolylineOverlay(
                    overlayId = "route-preview",
                    points = previewMap.polyline.map(GeoCoordinate::toMapCoordinate),
                    style = MapViewportPolylineStyle.ROUTE_PREVIEW,
                    tone = routeTone,
                ),
            ).filter(MapViewportPolylineOverlay::isRenderable),
    )

internal fun createNavigationViewportOverlayState(
    mapOverlay: NavigationMapOverlayUiState,
): MapViewportOverlayState {
    val useFocusedProjection = mapOverlay.mapFocusMode == NavigationMapFocusMode.FOCUSED

    return MapViewportOverlayState(
        points =
            buildList {
                mapOverlay.currentLocation?.let { point ->
                    add(
                        point.coordinate.toOverlayPoint(
                            overlayId = "navigation-current",
                            kind = MapViewportPointKind.CURRENT_LOCATION,
                            label = "C",
                            includeInProjection = !useFocusedProjection,
                        ),
                    )
                }
                mapOverlay.origin?.let { point ->
                    add(
                        point.coordinate.toOverlayPoint(
                            overlayId = "navigation-origin",
                            kind = MapViewportPointKind.ORIGIN,
                            label = "O",
                            includeInProjection = !useFocusedProjection,
                        ),
                    )
                }
                mapOverlay.destination?.let { point ->
                    add(
                        point.coordinate.toOverlayPoint(
                            overlayId = "navigation-destination",
                            kind = MapViewportPointKind.DESTINATION,
                            label = "D",
                            includeInProjection = !useFocusedProjection,
                        ),
                    )
                }
                addAll(mapOverlay.routeSegments.toSegmentMarkerOverlays())
                if (useFocusedProjection) {
                    mapOverlay.focusCoordinate?.let { coordinate ->
                        add(
                            coordinate.toOverlayPoint(
                                overlayId = "navigation-focus",
                                kind = MapViewportPointKind.FOCUS_HALO,
                                includeInProjection = true,
                            ),
                        )
                    }
                }
            },
        polylines =
            buildList<MapViewportPolylineOverlay> {
                addAll(
                    mapOverlay.routeSegments.toBaselinePolylineOverlays(
                        includeInProjection = !useFocusedProjection,
                    ),
                )
                if (this.none { overlay -> overlay.style == MapViewportPolylineStyle.ROUTE_BASELINE }) {
                    add(
                        MapViewportPolylineOverlay(
                            overlayId = "navigation-route",
                            points = mapOverlay.selectedRoutePolyline.map(GeoCoordinate::toMapCoordinate),
                            style = MapViewportPolylineStyle.ROUTE_BASELINE,
                            tone = MapViewportOverlayTone.PRIMARY,
                            includeInProjection = !useFocusedProjection,
                        ),
                    )
                }
                if (mapOverlay.activeSegmentPolyline != mapOverlay.focusedSegmentPolyline) {
                    add(
                        MapViewportPolylineOverlay(
                            overlayId = "navigation-active",
                            points = mapOverlay.activeSegmentPolyline.map(GeoCoordinate::toMapCoordinate),
                            style = MapViewportPolylineStyle.ACTIVE_SEGMENT,
                            tone = mapOverlay.activeSegmentTravelKind.toActiveOverlayTone(),
                            includeInProjection = !useFocusedProjection,
                        ),
                    )
                }
                add(
                    MapViewportPolylineOverlay(
                        overlayId = "navigation-focused",
                        points = mapOverlay.focusedSegmentPolyline.map(GeoCoordinate::toMapCoordinate),
                        style = MapViewportPolylineStyle.FOCUSED_SEGMENT,
                        tone = mapOverlay.focusedSegmentTravelKind.toFocusedOverlayTone(),
                        includeInProjection = !useFocusedProjection,
                    ),
                )
            }.filter(MapViewportPolylineOverlay::isRenderable),
    )
}

private fun defaultMapViewportFallbackCamera(): MapViewportFallbackCamera =
    MapViewportFallbackCamera(
        center = MapCoordinate(latitude = DEFAULT_VIEWPORT_CENTER_LATITUDE, longitude = DEFAULT_VIEWPORT_CENTER_LONGITUDE),
        latitudeSpan = MIN_VIEWPORT_LATITUDE_SPAN,
        longitudeSpan = MIN_VIEWPORT_LONGITUDE_SPAN,
    )

private fun MapCameraTarget.toViewportFallbackCamera(): MapViewportFallbackCamera =
    MapViewportFallbackCamera(
        center = center,
        latitudeSpan =
            when (source) {
                MapCameraSource.DEFAULT_BUSAN -> 0.010
                MapCameraSource.CURRENT_LOCATION -> 0.006
                MapCameraSource.SEARCH_RESULT -> 0.004
            },
        longitudeSpan =
            when (source) {
                MapCameraSource.DEFAULT_BUSAN -> 0.014
                MapCameraSource.CURRENT_LOCATION -> 0.009
                MapCameraSource.SEARCH_RESULT -> 0.007
            },
    )

private fun GeoCoordinate.toOverlayPoint(
    overlayId: String,
    kind: MapViewportPointKind,
    label: String? = null,
    includeInProjection: Boolean = true,
): MapViewportPointOverlay =
    MapViewportPointOverlay(
        overlayId = overlayId,
        coordinate = toMapCoordinate(),
        kind = kind,
        label = label,
        contentDescription = label,
        includeInProjection = includeInProjection,
    )

private fun GeoCoordinate.toMapCoordinate(): MapCoordinate =
    MapCoordinate(
        latitude = latitude,
        longitude = longitude,
    )

private fun List<NavigationMapSegmentUiState>.toSegmentMarkerOverlays(): List<MapViewportPointOverlay> =
    mapIndexedNotNull { index, segment ->
        if (index == 0 || !segment.isRenderable) return@mapIndexedNotNull null
        val coordinate = segment.polyline.firstOrNull() ?: return@mapIndexedNotNull null
        MapViewportPointOverlay(
            overlayId = "navigation-junction-$index",
            coordinate = coordinate.toMapCoordinate(),
            kind = MapViewportPointKind.SEGMENT_JUNCTION,
            tone = segment.travelKind.toSegmentMarkerTone(),
            includeInProjection = false,
        )
    }

private fun List<NavigationMapSegmentUiState>.toBaselinePolylineOverlays(
    includeInProjection: Boolean,
): List<MapViewportPolylineOverlay> =
    map { segment ->
        MapViewportPolylineOverlay(
            overlayId = "navigation-route-segment-${segment.sequence}",
            points = segment.polyline.map(GeoCoordinate::toMapCoordinate),
            style = MapViewportPolylineStyle.ROUTE_BASELINE,
            tone = segment.travelKind.toBaselineOverlayTone(),
            includeInProjection = includeInProjection,
        )
    }.filter(MapViewportPolylineOverlay::isRenderable)

private fun NavigationSegmentTravelKind.toBaselineOverlayTone(): MapViewportOverlayTone =
    when (this) {
        NavigationSegmentTravelKind.WALK -> MapViewportOverlayTone.PRIMARY
        NavigationSegmentTravelKind.TRANSIT -> MapViewportOverlayTone.TERTIARY
    }

private fun NavigationSegmentTravelKind.toSegmentMarkerTone(): MapViewportOverlayTone =
    when (this) {
        NavigationSegmentTravelKind.WALK -> MapViewportOverlayTone.PRIMARY
        NavigationSegmentTravelKind.TRANSIT -> MapViewportOverlayTone.TERTIARY
    }

private fun NavigationSegmentTravelKind.toActiveOverlayTone(): MapViewportOverlayTone =
    when (this) {
        NavigationSegmentTravelKind.WALK -> MapViewportOverlayTone.SECONDARY
        NavigationSegmentTravelKind.TRANSIT -> MapViewportOverlayTone.TERTIARY
    }

private fun NavigationSegmentTravelKind.toFocusedOverlayTone(): MapViewportOverlayTone =
    when (this) {
        NavigationSegmentTravelKind.WALK -> MapViewportOverlayTone.PRIMARY
        NavigationSegmentTravelKind.TRANSIT -> MapViewportOverlayTone.TERTIARY
    }

private fun RouteOption?.toViewportOverlayTone(): MapViewportOverlayTone =
    when (this) {
        RouteOption.SHORTEST,
        RouteOption.MIN_WALK,
            -> MapViewportOverlayTone.TERTIARY

        RouteOption.MIN_TRANSFER -> MapViewportOverlayTone.SECONDARY
        RouteOption.SAFE,
        RouteOption.RECOMMENDED,
        null,
            -> MapViewportOverlayTone.PRIMARY
    }

internal fun MapViewportOverlayTone.toSegmentMarkerPalette(): MapViewportSegmentMarkerPalette =
    when (this) {
        MapViewportOverlayTone.PRIMARY ->
            MapViewportSegmentMarkerPalette(
                fillColorArgb = 0xFF2A7BFF.toInt(),
                strokeColorArgb = 0xFF0F4FC6.toInt(),
            )

        MapViewportOverlayTone.SECONDARY ->
            MapViewportSegmentMarkerPalette(
                fillColorArgb = 0xFF14AA82.toInt(),
                strokeColorArgb = 0xFF0A7B5E.toInt(),
            )

        MapViewportOverlayTone.TERTIARY ->
            MapViewportSegmentMarkerPalette(
                fillColorArgb = 0xFFE7832F.toInt(),
                strokeColorArgb = 0xFFB85B16.toInt(),
            )

        MapViewportOverlayTone.ERROR ->
            MapViewportSegmentMarkerPalette(
                fillColorArgb = 0xFFD94C4C.toInt(),
                strokeColorArgb = 0xFF9D2A2A.toInt(),
            )
    }

internal const val DEFAULT_VIEWPORT_CENTER_LATITUDE = 35.1796
internal const val DEFAULT_VIEWPORT_CENTER_LONGITUDE = 129.0756
internal const val MIN_VIEWPORT_LATITUDE_SPAN = 0.0035
internal const val MIN_VIEWPORT_LONGITUDE_SPAN = 0.0045
