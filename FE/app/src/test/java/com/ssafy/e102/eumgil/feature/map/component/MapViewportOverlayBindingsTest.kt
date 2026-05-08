package com.ssafy.e102.eumgil.feature.map.component

import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerCategoryType
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerDisplayState
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerLoadStatus
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerOverlayState
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerUiModel
import com.ssafy.e102.eumgil.feature.navigation.NavigationMapFocusMode
import com.ssafy.e102.eumgil.feature.navigation.NavigationMapOverlayUiState
import com.ssafy.e102.eumgil.feature.navigation.NavigationMapPointUiState
import com.ssafy.e102.eumgil.feature.route.RoutePreviewMapStatus
import com.ssafy.e102.eumgil.feature.route.RoutePreviewMapUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewportOverlayBindingsTest {
    @Test
    fun `marker overlay binding keeps facility markers and camera focus separate`() {
        val cameraTarget =
            MapCameraTarget(
                center = MapCoordinate(latitude = 35.1796, longitude = 129.0756),
                source = MapCameraSource.CURRENT_LOCATION,
                requestId = 11L,
            )
        val overlayState =
            createMapMarkerViewportOverlayState(
                cameraTarget = cameraTarget,
                markerOverlayState =
                    MapMarkerOverlayState(
                        loadStatus = MapMarkerLoadStatus.READY,
                        markers =
                            listOf(
                                MapMarkerUiModel(
                                    markerId = "hidden",
                                    name = "Hidden",
                                    coordinate = MapCoordinate(latitude = 35.10, longitude = 129.10),
                                    categoryType = MapMarkerCategoryType(category = FacilityCategory.OTHER),
                                    displayState = MapMarkerDisplayState.HIDDEN_BY_FILTER,
                                ),
                                MapMarkerUiModel(
                                    markerId = "toilet",
                                    name = "Accessible toilet",
                                    coordinate = MapCoordinate(latitude = 35.18, longitude = 129.07),
                                    categoryType = MapMarkerCategoryType(category = FacilityCategory.TOILET),
                                ),
                                MapMarkerUiModel(
                                    markerId = "elevator",
                                    name = "Elevator",
                                    coordinate = MapCoordinate(latitude = 35.181, longitude = 129.071),
                                    categoryType = MapMarkerCategoryType(category = FacilityCategory.ELEVATOR),
                                ),
                            ),
                        visibleMarkerCount = 2,
                        totalMarkerCount = 3,
                    ),
                selectedMarkerId = "elevator",
            )

        assertEquals(cameraTarget.center, overlayState.fallbackCamera.center)
        assertTrue(overlayState.polylines.isEmpty())
        assertEquals(3, overlayState.points.size)
        assertEquals(MapViewportPointKind.CAMERA_FOCUS, overlayState.points.first().kind)
        assertEquals(listOf("toilet", "elevator"), overlayState.points.drop(1).map { it.overlayId })
        assertEquals(MapViewportPointKind.FACILITY, overlayState.points[1].kind)
        assertEquals(FacilityCategory.TOILET, overlayState.points[1].categoryType?.category)
        assertFalse(overlayState.points[1].isSelected)
        assertTrue(overlayState.points[2].isSelected)
        assertEquals("elevator", overlayState.points[2].clickTargetId)
    }

    @Test
    fun `route preview binding exposes origin destination and preview polyline`() {
        val overlayState =
            createRoutePreviewViewportOverlayState(
                previewMap =
                    RoutePreviewMapUiState(
                        status = RoutePreviewMapStatus.READY,
                        originCoordinate = GeoCoordinate(latitude = 35.17, longitude = 129.05),
                        destinationCoordinate = GeoCoordinate(latitude = 35.18, longitude = 129.07),
                        polyline =
                            listOf(
                                GeoCoordinate(latitude = 35.17, longitude = 129.05),
                                GeoCoordinate(latitude = 35.175, longitude = 129.06),
                                GeoCoordinate(latitude = 35.18, longitude = 129.07),
                            ),
                    ),
                routeTone = MapViewportOverlayTone.PRIMARY,
            )

        assertEquals(2, overlayState.points.size)
        assertEquals(
            listOf(MapViewportPointKind.ORIGIN, MapViewportPointKind.DESTINATION),
            overlayState.points.map { it.kind },
        )
        assertEquals(1, overlayState.polylines.size)
        assertEquals(MapViewportPolylineStyle.ROUTE_PREVIEW, overlayState.polylines.first().style)
        assertEquals(MapViewportOverlayTone.PRIMARY, overlayState.polylines.first().tone)
        assertTrue(overlayState.polylines.first().includeInProjection)
    }

    @Test
    fun `navigation binding drops full route from focused projection but keeps active overlays`() {
        val overlayState =
            createNavigationViewportOverlayState(
                mapOverlay =
                    NavigationMapOverlayUiState(
                        isDisplayable = true,
                        currentLocation =
                            NavigationMapPointUiState(
                                label = "Current",
                                coordinate = GeoCoordinate(latitude = 35.176, longitude = 129.061),
                            ),
                        origin =
                            NavigationMapPointUiState(
                                label = "Origin",
                                coordinate = GeoCoordinate(latitude = 35.170, longitude = 129.050),
                            ),
                        destination =
                            NavigationMapPointUiState(
                                label = "Destination",
                                coordinate = GeoCoordinate(latitude = 35.190, longitude = 129.080),
                            ),
                        selectedRoutePolyline =
                            listOf(
                                GeoCoordinate(latitude = 35.170, longitude = 129.050),
                                GeoCoordinate(latitude = 35.180, longitude = 129.065),
                                GeoCoordinate(latitude = 35.190, longitude = 129.080),
                            ),
                        activeSegmentPolyline =
                            listOf(
                                GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                GeoCoordinate(latitude = 35.178, longitude = 129.063),
                            ),
                        focusedSegmentPolyline =
                            listOf(
                                GeoCoordinate(latitude = 35.178, longitude = 129.063),
                                GeoCoordinate(latitude = 35.181, longitude = 129.068),
                            ),
                        focusCoordinate = GeoCoordinate(latitude = 35.1795, longitude = 129.0655),
                        mapFocusMode = NavigationMapFocusMode.FOCUSED,
                    ),
            )

        assertEquals(
            listOf(
                MapViewportPolylineStyle.ROUTE_BASELINE,
                MapViewportPolylineStyle.ACTIVE_SEGMENT,
                MapViewportPolylineStyle.FOCUSED_SEGMENT,
            ),
            overlayState.polylines.map { it.style },
        )
        assertFalse(overlayState.polylines.first().includeInProjection)
        assertTrue(overlayState.polylines.drop(1).all { it.includeInProjection })
        assertEquals(
            listOf(
                MapViewportPointKind.CURRENT_LOCATION,
                MapViewportPointKind.ORIGIN,
                MapViewportPointKind.DESTINATION,
                MapViewportPointKind.FOCUS_HALO,
            ),
            overlayState.points.map { it.kind },
        )
        assertTrue(overlayState.points[0].includeInProjection)
        assertFalse(overlayState.points[1].includeInProjection)
        assertFalse(overlayState.points[2].includeInProjection)
        assertTrue(overlayState.points[3].includeInProjection)
    }
}
