package com.ssafy.e102.eumgil.feature.map.component

import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
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
import com.ssafy.e102.eumgil.feature.navigation.NavigationMapSegmentUiState
import com.ssafy.e102.eumgil.feature.navigation.NavigationSegmentTravelKind
import com.ssafy.e102.eumgil.feature.navigation.navigationSegmentMarkerId
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
                currentLocation = null,
                currentLocationLabel = null,
            )

        assertEquals(cameraTarget.center, overlayState.fallbackCamera.center)
        assertTrue(overlayState.polylines.isEmpty())
        assertEquals(3, overlayState.points.size)
        assertEquals(MapViewportPointKind.CAMERA_FOCUS, overlayState.points.first().kind)
        assertEquals(listOf("toilet", "elevator"), overlayState.points.drop(1).map { it.overlayId })
        assertEquals(MapViewportPointKind.FACILITY, overlayState.points[1].kind)
        assertEquals(FacilityCategory.TOILET, overlayState.points[1].categoryType?.category)
        assertFalse(overlayState.points[1].isSelected)
        assertFalse(overlayState.points[1].includeInProjection)
        assertTrue(overlayState.points[2].isSelected)
        assertFalse(overlayState.points[2].includeInProjection)
        assertEquals("elevator", overlayState.points[2].clickTargetId)
        assertEquals(null, createKakaoRouteCameraRenderState(overlayState))
    }

    @Test
    fun `marker overlay binding adds current location point when location is ready`() {
        val cameraTarget =
            MapCameraTarget(
                center = MapCoordinate(latitude = 35.1796, longitude = 129.0756),
                source = MapCameraSource.CURRENT_LOCATION,
                requestId = 12L,
            )
        val currentLocation = MapCoordinate(latitude = 35.1798, longitude = 129.0762)

        val overlayState =
            createMapMarkerViewportOverlayState(
                cameraTarget = cameraTarget,
                markerOverlayState =
                    MapMarkerOverlayState(
                        loadStatus = MapMarkerLoadStatus.READY,
                        markers =
                            listOf(
                                MapMarkerUiModel(
                                    markerId = "toilet",
                                    name = "Accessible toilet",
                                    coordinate = MapCoordinate(latitude = 35.18, longitude = 129.07),
                                    categoryType = MapMarkerCategoryType(category = FacilityCategory.TOILET),
                                ),
                            ),
                        visibleMarkerCount = 1,
                        totalMarkerCount = 1,
                    ),
                selectedMarkerId = null,
                currentLocation = currentLocation,
                currentLocationLabel = "현",
            )

        assertEquals(
            listOf(
                MapViewportPointKind.CAMERA_FOCUS,
                MapViewportPointKind.CURRENT_LOCATION,
                MapViewportPointKind.FACILITY,
            ),
            overlayState.points.map { it.kind },
        )
        assertEquals(currentLocation, overlayState.points[1].coordinate)
        assertEquals("현", overlayState.points[1].label)
        assertFalse(overlayState.points[1].includeInProjection)
        assertEquals("toilet", overlayState.points[2].overlayId)
        assertFalse(overlayState.points[2].includeInProjection)
        assertEquals(null, createKakaoRouteCameraRenderState(overlayState))
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
        assertTrue(overlayState.polylines.first().showDirectionArrows)
    }

    @Test
    fun `route preview hides detailed guidance markers and arrows until a guidance marker is focused`() {
        val previewMap =
            RoutePreviewMapUiState(
                status = RoutePreviewMapStatus.READY,
                originCoordinate = GeoCoordinate(latitude = 35.17, longitude = 129.05),
                destinationCoordinate = GeoCoordinate(latitude = 35.18, longitude = 129.07),
                polyline =
                    listOf(
                        GeoCoordinate(latitude = 35.17, longitude = 129.05),
                        GeoCoordinate(latitude = 35.18, longitude = 129.07),
                    ),
            )
        val transitMarker =
            MapViewportPointOverlay(
                overlayId = "bus-marker",
                coordinate = MapCoordinate(latitude = 35.175, longitude = 129.06),
                kind = MapViewportPointKind.TRANSIT_BUS_STOP,
                transitMarker =
                    MapViewportTransitMarker(
                        from = MapViewportTransitMarkerLeg(kind = MapViewportTransitMarkerKind.BUS),
                    ),
            )
        val genericMarker =
            transitMarker.copy(
                overlayId = "generic-marker",
                kind = MapViewportPointKind.SEGMENT_JUNCTION,
                transitMarker = null,
            )

        val initialState =
            createRoutePreviewViewportOverlayState(
                previewMap = previewMap,
                guidanceMarkers = listOf(genericMarker, transitMarker),
                showDetailedRouteOverlay = false,
            )
        val focusedState =
            createRoutePreviewViewportOverlayState(
                previewMap = previewMap,
                guidanceMarkers = listOf(genericMarker.copy(isSelected = true), transitMarker.copy(isSelected = true)),
                focusSelectedGuidanceMarker = true,
                showDetailedRouteOverlay = true,
            )

        assertEquals(
            listOf(MapViewportPointKind.ORIGIN, MapViewportPointKind.DESTINATION),
            initialState.points.map { it.kind },
        )
        assertFalse(initialState.polylines.first().showDirectionArrows)

        assertTrue(focusedState.points.any { it.kind == MapViewportPointKind.TRANSIT_BUS_STOP })
        assertFalse(focusedState.points.any { it.overlayId == "generic-marker" })
        assertTrue(focusedState.polylines.first().showDirectionArrows)
    }

    @Test
    fun `navigation binding includes the focused polyline in focused projection`() {
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
        assertFalse(overlayState.polylines[1].includeInProjection)
        assertTrue(overlayState.polylines[2].includeInProjection)
        assertFalse(overlayState.polylines[0].showDirectionArrows)
        assertFalse(overlayState.polylines[1].showDirectionArrows)
        assertTrue(overlayState.polylines[2].showDirectionArrows)
        assertEquals(
            listOf(
                MapViewportPointKind.CURRENT_LOCATION,
                MapViewportPointKind.ORIGIN,
                MapViewportPointKind.DESTINATION,
                MapViewportPointKind.FOCUS_HALO,
            ),
            overlayState.points.map { it.kind },
        )
        assertFalse(overlayState.points[0].includeInProjection)
        assertFalse(overlayState.points[1].includeInProjection)
        assertFalse(overlayState.points[2].includeInProjection)
        assertTrue(overlayState.points[3].includeInProjection)
        assertEquals(
            listOf(MapViewportPointKind.FOCUS_HALO),
            overlayState.points
                .filter { point -> point.includeInProjection }
                .map { point -> point.kind },
        )
    }

    @Test
    fun `navigation binding keeps active projection on current location instead of the route overview`() {
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
                                GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                GeoCoordinate(latitude = 35.178, longitude = 129.063),
                            ),
                        focusCoordinate = GeoCoordinate(latitude = 35.1765, longitude = 129.0605),
                        mapFocusMode = NavigationMapFocusMode.ACTIVE,
                    ),
            )
        val cameraState = createKakaoRouteCameraRenderState(overlayState)

        assertEquals(
            listOf(
                MapViewportPolylineStyle.ROUTE_BASELINE,
                MapViewportPolylineStyle.FOCUSED_SEGMENT,
            ),
            overlayState.polylines.map { it.style },
        )
        assertFalse(overlayState.polylines[0].includeInProjection)
        assertFalse(overlayState.polylines[1].includeInProjection)
        assertFalse(overlayState.polylines[0].showDirectionArrows)
        assertTrue(overlayState.polylines[1].showDirectionArrows)
        assertEquals(
            listOf(
                MapViewportPointKind.CURRENT_LOCATION,
                MapViewportPointKind.ORIGIN,
                MapViewportPointKind.DESTINATION,
                MapViewportPointKind.FOCUS_HALO,
            ),
            overlayState.points.map { it.kind },
        )
        assertEquals(
            listOf(true, false, false, false),
            overlayState.points.map { it.includeInProjection },
        )
        assertEquals(null, cameraState)
    }

    @Test
    fun `navigation binding adds segment start markers except the first segment and skips empty polylines`() {
        val overlayState =
            createNavigationViewportOverlayState(
                mapOverlay =
                    NavigationMapOverlayUiState(
                        isDisplayable = true,
                        routeSegments =
                            listOf(
                                NavigationMapSegmentUiState(
                                    sequence = 1,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.170, longitude = 129.050),
                                            GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                        ),
                                    distanceMeters = 300,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "First",
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 2,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                            GeoCoordinate(latitude = 35.181, longitude = 129.068),
                                        ),
                                    distanceMeters = 320,
                                    riskLevel = RouteRiskLevel.MEDIUM,
                                    guidanceMessage = "Second",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 3,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.181, longitude = 129.068),
                                            GeoCoordinate(latitude = 35.190, longitude = 129.080),
                                        ),
                                    distanceMeters = 400,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Third",
                                    travelKind = NavigationSegmentTravelKind.TRANSIT,
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 4,
                                    polyline = emptyList(),
                                    distanceMeters = 40,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Ignored",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                            ),
                    ),
            )

        val junctionPoints = overlayState.points.filter { it.kind == MapViewportPointKind.SEGMENT_JUNCTION }
        assertEquals(1, junctionPoints.size)
        assertEquals(
            listOf(
                MapCoordinate(latitude = 35.175, longitude = 129.058),
            ),
            junctionPoints.map { it.coordinate },
        )
        assertEquals(
            listOf(MapViewportOverlayTone.NEUTRAL),
            junctionPoints.map { it.tone },
        )
        assertTrue(junctionPoints.none { it.includeInProjection })
    }

    @Test
    fun `navigation binding keeps single coordinate segment start markers when a start point exists`() {
        val overlayState =
            createNavigationViewportOverlayState(
                mapOverlay =
                    NavigationMapOverlayUiState(
                        isDisplayable = true,
                        routeSegments =
                            listOf(
                                NavigationMapSegmentUiState(
                                    sequence = 1,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.170, longitude = 129.050),
                                            GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                        ),
                                    distanceMeters = 300,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Hidden first",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 2,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.176, longitude = 129.060),
                                        ),
                                    distanceMeters = 120,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Single walk",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 3,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.181, longitude = 129.068),
                                        ),
                                    distanceMeters = 400,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Single transit",
                                    travelKind = NavigationSegmentTravelKind.TRANSIT,
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 4,
                                    polyline = emptyList(),
                                    distanceMeters = 40,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Empty",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                            ),
                    ),
            )

        val junctionPoints = overlayState.points.filter { it.kind == MapViewportPointKind.SEGMENT_JUNCTION }
        assertEquals(
            listOf(
                MapCoordinate(latitude = 35.176, longitude = 129.060),
            ),
            junctionPoints.map { it.coordinate },
        )
        assertEquals(
            listOf(MapViewportOverlayTone.NEUTRAL),
            junctionPoints.map { it.tone },
        )
    }

    @Test
    fun `navigation binding uses fallback segment start coordinates when later segment polylines are empty`() {
        val overlayState =
            createNavigationViewportOverlayState(
                mapOverlay =
                    NavigationMapOverlayUiState(
                        isDisplayable = true,
                        routeSegments =
                            listOf(
                                NavigationMapSegmentUiState(
                                    sequence = 1,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.170, longitude = 129.050),
                                            GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                        ),
                                    distanceMeters = 300,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "First",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 2,
                                    polyline = emptyList(),
                                    segmentStartCoordinate = GeoCoordinate(latitude = 35.176, longitude = 129.060),
                                    distanceMeters = 120,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Sparse walk",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 3,
                                    polyline = emptyList(),
                                    distanceMeters = 200,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Still empty",
                                    travelKind = NavigationSegmentTravelKind.TRANSIT,
                                ),
                            ),
                    ),
            )

        val junctionPoints = overlayState.points.filter { it.kind == MapViewportPointKind.SEGMENT_JUNCTION }
        assertEquals(1, junctionPoints.size)
        assertEquals(
            MapCoordinate(latitude = 35.176, longitude = 129.060),
            junctionPoints.single().coordinate,
        )
        assertEquals(MapViewportOverlayTone.NEUTRAL, junctionPoints.single().tone)
    }

    @Test
    fun `navigation binding keeps single coordinate segment junctions outside active camera projection`() {
        val overlayState =
            createNavigationViewportOverlayState(
                mapOverlay =
                    NavigationMapOverlayUiState(
                        isDisplayable = true,
                        routeSegments =
                            listOf(
                                NavigationMapSegmentUiState(
                                    sequence = 1,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.170, longitude = 129.050),
                                            GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                        ),
                                    distanceMeters = 300,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "First",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 2,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.176, longitude = 129.060),
                                        ),
                                    distanceMeters = 120,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Single walk",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                            ),
                        mapFocusMode = NavigationMapFocusMode.ACTIVE,
                    ),
            )

        assertTrue(overlayState.points.any { it.kind == MapViewportPointKind.SEGMENT_JUNCTION })
        assertTrue(overlayState.points.none { it.kind == MapViewportPointKind.SEGMENT_JUNCTION && it.includeInProjection })
    }

    @Test
    fun `navigation binding keeps the focus halo as the only projected point in focused mode`() {
        val overlayState =
            createNavigationViewportOverlayState(
                mapOverlay =
                    NavigationMapOverlayUiState(
                        isDisplayable = true,
                        routeSegments =
                            listOf(
                                NavigationMapSegmentUiState(
                                    sequence = 1,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.170, longitude = 129.050),
                                            GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                        ),
                                    distanceMeters = 300,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "First",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 2,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.176, longitude = 129.060),
                                            GeoCoordinate(latitude = 35.181, longitude = 129.068),
                                        ),
                                    distanceMeters = 400,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Focused transit",
                                    travelKind = NavigationSegmentTravelKind.TRANSIT,
                                    isFocused = true,
                                ),
                            ),
                        focusCoordinate = GeoCoordinate(latitude = 35.176, longitude = 129.060),
                        mapFocusMode = NavigationMapFocusMode.FOCUSED,
                    ),
            )

        val projectionPoints = overlayState.points.filter { it.includeInProjection }

        assertEquals(
            listOf(
                MapViewportPointKind.FOCUS_HALO,
            ),
            projectionPoints.map { it.kind },
        )
        assertEquals(MapCoordinate(latitude = 35.176, longitude = 129.060), projectionPoints.first().coordinate)
        assertTrue(
            overlayState.polylines.any { polyline ->
                polyline.overlayId == "navigation-focused" && polyline.includeInProjection
            },
        )
    }

    @Test
    fun `navigation binding falls back to the active focus halo when current location is unavailable`() {
        val overlayState =
            createNavigationViewportOverlayState(
                mapOverlay =
                    NavigationMapOverlayUiState(
                        isDisplayable = true,
                        routeSegments =
                            listOf(
                                NavigationMapSegmentUiState(
                                    sequence = 1,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.170, longitude = 129.050),
                                            GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                        ),
                                    distanceMeters = 300,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "First",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 2,
                                    polyline = emptyList(),
                                    segmentStartCoordinate = GeoCoordinate(latitude = 35.176, longitude = 129.060),
                                    distanceMeters = 120,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Sparse walk",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                            ),
                        focusCoordinate = GeoCoordinate(latitude = 35.176, longitude = 129.060),
                        mapFocusMode = NavigationMapFocusMode.ACTIVE,
                    ),
            )

        assertEquals(
            listOf(MapViewportPointKind.FOCUS_HALO),
            overlayState.points
                .filter(MapViewportPointOverlay::includeInProjection)
                .map(MapViewportPointOverlay::kind),
        )
        assertEquals(null, createKakaoRouteCameraRenderState(overlayState))
    }

    @Test
    fun `segment junction debug summary reports generated overlays and focus projection exclusions`() {
        val mapOverlay =
            NavigationMapOverlayUiState(
                isDisplayable = true,
                currentLocation =
                    NavigationMapPointUiState(
                        label = "Current",
                        coordinate = GeoCoordinate(latitude = 35.170, longitude = 129.050),
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
                routeSegments =
                    listOf(
                        NavigationMapSegmentUiState(
                            sequence = 1,
                            polyline =
                                listOf(
                                    GeoCoordinate(latitude = 35.170, longitude = 129.050),
                                    GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                ),
                            distanceMeters = 300,
                            riskLevel = RouteRiskLevel.LOW,
                            guidanceMessage = "Hidden first",
                            travelKind = NavigationSegmentTravelKind.WALK,
                        ),
                        NavigationMapSegmentUiState(
                            sequence = 2,
                            polyline =
                                listOf(
                                    GeoCoordinate(latitude = 35.176, longitude = 129.060),
                                ),
                            distanceMeters = 120,
                            riskLevel = RouteRiskLevel.LOW,
                            guidanceMessage = "Single walk",
                            travelKind = NavigationSegmentTravelKind.WALK,
                        ),
                        NavigationMapSegmentUiState(
                            sequence = 3,
                            polyline =
                                listOf(
                                    GeoCoordinate(latitude = 35.181, longitude = 129.068),
                                ),
                            distanceMeters = 400,
                            riskLevel = RouteRiskLevel.LOW,
                            guidanceMessage = "Single transit",
                            travelKind = NavigationSegmentTravelKind.TRANSIT,
                        ),
                    ),
                selectedRoutePolyline =
                    listOf(
                        GeoCoordinate(latitude = 35.170, longitude = 129.050),
                        GeoCoordinate(latitude = 35.190, longitude = 129.080),
                    ),
                activeSegmentPolyline =
                    listOf(
                        GeoCoordinate(latitude = 35.170, longitude = 129.050),
                        GeoCoordinate(latitude = 35.175, longitude = 129.058),
                    ),
                focusedSegmentPolyline =
                    listOf(
                        GeoCoordinate(latitude = 35.176, longitude = 129.060),
                        GeoCoordinate(latitude = 35.181, longitude = 129.068),
                    ),
                focusCoordinate = GeoCoordinate(latitude = 35.176, longitude = 129.060),
                mapFocusMode = NavigationMapFocusMode.FOCUSED,
            )
        val overlayState = createNavigationViewportOverlayState(mapOverlay)

        val summary = createSegmentJunctionOverlayDebugSummary(mapOverlay, overlayState)

        assertTrue(summary.contains("focusMode=FOCUSED"))
        assertTrue(summary.contains("junctions=1"))
        assertTrue(
            summary.contains(
                "id=navigation-junction-1 coord=35.176000,129.060000 tone=NEUTRAL includeInProjection=false",
            ),
        )
        assertTrue(summary.contains("projectionPoints=[navigation-focus:FOCUS_HALO]"))
        assertTrue(summary.contains("projectionPolylines=[navigation-focused:FOCUSED_SEGMENT]"))
    }

    @Test
    fun `navigation binding colors transit segments differently from walking segments`() {
        val overlayState =
            createNavigationViewportOverlayState(
                mapOverlay =
                    NavigationMapOverlayUiState(
                        isDisplayable = true,
                        routeSegments =
                            listOf(
                                NavigationMapSegmentUiState(
                                    sequence = 1,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.170, longitude = 129.050),
                                            GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                        ),
                                    distanceMeters = 300,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Walk",
                                    travelKind = NavigationSegmentTravelKind.WALK,
                                ),
                                NavigationMapSegmentUiState(
                                    sequence = 2,
                                    polyline =
                                        listOf(
                                            GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                            GeoCoordinate(latitude = 35.181, longitude = 129.068),
                                        ),
                                    distanceMeters = 320,
                                    riskLevel = RouteRiskLevel.LOW,
                                    guidanceMessage = "Transit",
                                    travelKind = NavigationSegmentTravelKind.TRANSIT,
                                ),
                            ),
                        activeSegmentPolyline =
                            listOf(
                                GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                GeoCoordinate(latitude = 35.181, longitude = 129.068),
                            ),
                        focusedSegmentPolyline =
                            listOf(
                                GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                GeoCoordinate(latitude = 35.181, longitude = 129.068),
                            ),
                        activeSegmentTravelKind = NavigationSegmentTravelKind.TRANSIT,
                        focusedSegmentTravelKind = NavigationSegmentTravelKind.TRANSIT,
                    ),
            )

        val baselineTones =
            overlayState.polylines
                .filter { it.style == MapViewportPolylineStyle.ROUTE_BASELINE }
                .map { it.tone }

        assertEquals(
            listOf(MapViewportOverlayTone.NEUTRAL, MapViewportOverlayTone.NAVY),
            baselineTones,
        )
        assertEquals(MapViewportOverlayTone.NAVY, overlayState.polylines.last().tone)
    }

    @Test
    fun `navigation bindings preserve click targets for projected origin and overlay junction markers`() {
        val mapOverlay =
            NavigationMapOverlayUiState(
                isDisplayable = true,
                origin =
                    NavigationMapPointUiState(
                        label = "Origin",
                        coordinate = GeoCoordinate(latitude = 35.170, longitude = 129.050),
                    ),
                routeSegments =
                    listOf(
                        NavigationMapSegmentUiState(
                            sequence = 1,
                            polyline =
                                listOf(
                                    GeoCoordinate(latitude = 35.170, longitude = 129.050),
                                    GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                ),
                            distanceMeters = 300,
                            riskLevel = RouteRiskLevel.LOW,
                            guidanceMessage = "Start walking",
                            travelKind = NavigationSegmentTravelKind.WALK,
                        ),
                        NavigationMapSegmentUiState(
                            sequence = 2,
                            polyline =
                                listOf(
                                    GeoCoordinate(latitude = 35.175, longitude = 129.058),
                                    GeoCoordinate(latitude = 35.181, longitude = 129.068),
                                ),
                            distanceMeters = 320,
                            riskLevel = RouteRiskLevel.LOW,
                            guidanceMessage = "Turn right",
                            travelKind = NavigationSegmentTravelKind.WALK,
                        ),
                    ),
            )
        val overlayState = createNavigationViewportOverlayState(mapOverlay)

        val projectedOriginMarker =
            createKakaoProjectedMarkerRenderStates(
                currentLocation = null,
                selectedDestinationCoordinate = null,
                selectedMapPinCoordinate = null,
                overlayPoints = overlayState.points,
            ).first { marker -> marker.markerId == "overlay-navigation-origin" }
        val overlayJunctionMarker =
            createKakaoOverlayMarkerRenderStates(
                overlayPoints = overlayState.points,
            ).first { marker -> marker.markerId == "overlay-navigation-junction-1" }

        assertEquals(navigationSegmentMarkerId(0), projectedOriginMarker.clickTargetId)
        assertEquals(navigationSegmentMarkerId(1), overlayJunctionMarker.clickTargetId)
    }
}
