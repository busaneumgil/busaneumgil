package com.ssafy.e102.eumgil.data.route

import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteTransportMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteSearchDtoMapperTest {
    private val geometryParser: RouteGeometryParser = DefaultRouteGeometryParser()

    @Test
    fun `toDomain maps walk route payload into search id legs steps and compatibility segments`() {
        val result =
            RouteSearchResponseDto(
                searchId = "rs_walk_123",
                routes =
                    listOf(
                        RouteDto(
                            routeId = "walk_rt_safe_001",
                            transportMode = "WALK",
                            routeOption = "SAFE",
                            title = "Accessible Walk",
                            distanceMeter = 120.0,
                            estimatedTimeMinute = 2,
                            badges = listOf("LOW_SLOPE"),
                            geometry = "LINESTRING(129.075600 35.179600, 129.076800 35.180600)",
                            legs =
                                listOf(
                                    RouteLegDto(
                                        sequence = 1,
                                        type = "WALK",
                                        role = "WALK_ONLY",
                                        instruction = "Walk to destination",
                                        distanceMeter = 120.0,
                                        estimatedTimeMinute = 2,
                                        geometry = "LINESTRING(129.075600 35.179600, 129.076800 35.180600)",
                                        steps =
                                            listOf(
                                                RouteStepDto(
                                                    sequence = 1,
                                                    instruction = "Go straight",
                                                    distanceMeter = 80.0,
                                                    geometry =
                                                        "LINESTRING(129.075600 35.179600, 129.076000 35.179900)",
                                                    badges = listOf("LOW_SLOPE"),
                                                    alerts =
                                                        listOf(
                                                            RouteAlertDto(
                                                                type = "CROSSWALK",
                                                                distanceMeter = 15.0,
                                                            ),
                                                        ),
                                                    slopePercent = 1.8,
                                                    widthState = "WIDE",
                                                ),
                                                RouteStepDto(
                                                    sequence = 2,
                                                    instruction = "Cross the street",
                                                    distanceMeter = 40.0,
                                                    geometry = "POINT(129.076800 35.180600)",
                                                    badges = listOf("CROSSWALK"),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            ).toDomain(
                query = testRouteSearchQuery(routeOptions = listOf("SAFE")),
                geometryParser = geometryParser,
            )

        assertEquals("rs_walk_123", result.searchId)

        val route = result.routes.single()
        assertEquals("walk_rt_safe_001", route.routeId)
        assertEquals("walk_rt_safe_001", route.serverRouteId)
        assertEquals(RouteTransportMode.WALK, route.transportMode)
        assertEquals(RouteOption.SAFE, route.routeOption)
        assertEquals(listOf("LOW_SLOPE"), route.badges.map { badge -> badge.name })
        assertEquals(1, route.legs.size)
        assertEquals(2, route.legs.single().steps.size)
        assertEquals(2, route.segments.size)
        assertEquals("Go straight", route.segments.first().guidanceMessage)
        assertEquals(2, route.preview.segmentCount)
        assertEquals(1, route.preview.renderableSegmentCount)
        assertEquals(1, route.preview.fallbackSegmentCount)
        assertTrue(route.hasRenderablePreview)
    }

    @Test
    fun `toDomain maps public transit routes with transfer count and transit leg metadata`() {
        val result =
            RouteSearchResponseDto(
                searchId = "rs_transit_123",
                routes =
                    listOf(
                        RouteDto(
                            routeId = "pt_rt_001",
                            transportMode = "PUBLIC_TRANSIT",
                            routeOption = "RECOMMENDED",
                            title = "Bus plus walk",
                            distanceMeter = 4200.0,
                            estimatedTimeMinute = 27,
                            transferCount = 1,
                            badges = listOf("LOW_SLOPE", "CROSSWALK"),
                            legs =
                                listOf(
                                    RouteLegDto(
                                        sequence = 1,
                                        type = "WALK",
                                        role = "WALK_TO_TRANSIT",
                                        instruction = "Walk to stop A",
                                        distanceMeter = 180.0,
                                        estimatedTimeMinute = 3,
                                        geometry =
                                            "LINESTRING(129.075600 35.179600, 129.076000 35.179900)",
                                        steps =
                                            listOf(
                                                RouteStepDto(
                                                    sequence = 1,
                                                    instruction = "Walk to stop A",
                                                    distanceMeter = 180.0,
                                                    geometry =
                                                        "LINESTRING(129.075600 35.179600, 129.076000 35.179900)",
                                                    badges = listOf("LOW_SLOPE"),
                                                ),
                                            ),
                                    ),
                                    RouteLegDto(
                                        sequence = 2,
                                        type = "BUS",
                                        role = "TRANSIT",
                                        instruction = "Take bus 100",
                                        estimatedTimeMinute = 11,
                                        routeNo = "100",
                                        boardingStop =
                                            RouteTransitStopDto(
                                                name = "Stop A",
                                                lat = 35.1799,
                                                lng = 129.0760,
                                            ),
                                        alightingStop =
                                            RouteTransitStopDto(
                                                name = "Stop B",
                                                lat = 35.1650,
                                                lng = 129.0600,
                                            ),
                                    ),
                                ),
                        ),
                    ),
            ).toDomain(
                query = testRouteSearchQuery(routeOptions = listOf("SAFE")),
                geometryParser = geometryParser,
            )

        assertEquals("rs_transit_123", result.searchId)

        val route = result.routes.single()
        assertEquals("pt_rt_001", route.routeId)
        assertEquals(RouteTransportMode.PUBLIC_TRANSIT, route.transportMode)
        assertEquals(RouteOption.RECOMMENDED, route.routeOption)
        assertEquals(1, route.transferCount)
        assertEquals(2, route.legs.size)
        assertEquals("BUS", route.legs.last().type.name)
        assertEquals("100", route.legs.last().routeNo)
        assertEquals("Stop A", route.legs.last().boardingStop?.name)
        assertEquals("Stop B", route.legs.last().alightingStop?.name)
        assertEquals(2, route.segments.size)
        assertEquals("Take bus 100", route.segments.last().guidanceMessage)
    }

    @Test
    fun `toDomain falls back to leg geometry when walk payload omits steps`() {
        val result =
            RouteSearchResponseDto(
                searchId = "rs_walk_leg_only",
                routes =
                    listOf(
                        RouteDto(
                            routeId = "walk_rt_safe_fallback",
                            transportMode = "WALK",
                            routeOption = "SAFE",
                            title = "Leg fallback route",
                            distanceMeter = 120.0,
                            estimatedTimeMinute = 2,
                            geometry =
                                "LINESTRING(129.075600 35.179600, 129.076100 35.180000)",
                            legs =
                                listOf(
                                    RouteLegDto(
                                        sequence = 1,
                                        type = "WALK",
                                        role = "WALK_ONLY",
                                        instruction = "Continue straight",
                                        distanceMeter = 120.0,
                                        estimatedTimeMinute = 2,
                                        geometry =
                                            "LINESTRING(129.075600 35.179600, 129.076100 35.180000)",
                                    ),
                                ),
                        ),
                    ),
            ).toDomain(
                query =
                    testRouteSearchQuery(
                        routeOptions = listOf("SAFE"),
                    ),
                geometryParser = geometryParser,
            )

        val route = result.routes.single()
        assertEquals("rs_walk_leg_only", result.searchId)
        assertEquals(1, route.segments.size)
        assertEquals("Continue straight", route.segments.single().guidanceMessage)
        assertEquals(1, route.preview.segmentCount)
        assertEquals(1, route.preview.renderableSegmentCount)
        assertEquals(0, route.preview.fallbackSegmentCount)
        assertTrue(route.hasRenderablePreview)
        assertEquals(route.preview.polyline, route.previewPolyline)
    }

    @Test
    fun `toDomain maps walk step alert crosswalk variants to safety flags`() {
        val result =
            RouteSearchResponseDto(
                routes =
                    listOf(
                        RouteDto(
                            routeOption = "SAFE",
                            title = "안전 경로",
                            distanceMeter = 90.0,
                            estimatedTimeMinute = 2,
                            legs =
                                listOf(
                                    RouteLegDto(
                                        steps =
                                            listOf(
                                                RouteStepDto(
                                                    sequence = 1,
                                                    instruction = "직진하세요.",
                                                    geometry = "LINESTRING(129.075600 35.179600, 129.076000 35.179900)",
                                                    distanceMeter = 30.0,
                                                    alert = RouteStepAlertDto(type = "CROSSWALK"),
                                                ),
                                                RouteStepDto(
                                                    sequence = 2,
                                                    instruction = "직진하세요.",
                                                    geometry = "LINESTRING(129.076000 35.179900, 129.077000 35.180500)",
                                                    distanceMeter = 30.0,
                                                    alert = RouteStepAlertDto(type = "CROSSWALK_SIGNAL"),
                                                ),
                                                RouteStepDto(
                                                    sequence = 3,
                                                    instruction = "직진하세요.",
                                                    geometry = "LINESTRING(129.077000 35.180500, 129.078000 35.181000)",
                                                    distanceMeter = 30.0,
                                                    alert = RouteStepAlertDto(type = "CROSSWALK_AUDIO"),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            ).toDomain(
                query = testRouteSearchQuery(routeOptions = listOf("SAFE")),
                geometryParser = geometryParser,
            )

        val flags = result.routes.single().segments.map { segment -> segment.safetyFlags }
        assertEquals(3, flags.size)
        assertTrue(flags[0].hasCrosswalk)
        assertFalse(flags[0].hasSignal)
        assertFalse(flags[0].hasAudioSignal)
        assertTrue(flags[1].hasCrosswalk)
        assertTrue(flags[1].hasSignal)
        assertFalse(flags[1].hasAudioSignal)
        assertTrue(flags[2].hasCrosswalk)
        assertTrue(flags[2].hasSignal)
        assertTrue(flags[2].hasAudioSignal)
    }
}

private fun testRouteSearchQuery(routeOptions: List<String>): com.ssafy.e102.eumgil.core.model.RouteSearchQuery =
    com.ssafy.e102.eumgil.core.model.RouteSearchQuery(
        origin =
            com.ssafy.e102.eumgil.core.model.RouteWaypoint(
                name = "Origin",
                coordinate =
                    com.ssafy.e102.eumgil.core.model.GeoCoordinate(
                        latitude = 35.1796,
                        longitude = 129.0756,
                    ),
            ),
        destination =
            com.ssafy.e102.eumgil.core.model.RouteWaypoint(
                name = "Destination",
                coordinate =
                    com.ssafy.e102.eumgil.core.model.GeoCoordinate(
                        latitude = 35.1151,
                        longitude = 129.0414,
                    ),
            ),
        requestedOptions =
            routeOptions.map { option ->
                com.ssafy.e102.eumgil.core.model.RouteOption.valueOf(option)
            },
    )
