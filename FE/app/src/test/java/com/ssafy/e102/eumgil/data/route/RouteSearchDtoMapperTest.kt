package com.ssafy.e102.eumgil.data.route

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteSearchDtoMapperTest {
    private val geometryParser: RouteGeometryParser = DefaultRouteGeometryParser()

    @Test
    fun `toRoutePreviewModel keeps only renderable segment lines while counting fallback segments`() {
        val preview =
            listOf(
                RouteSegmentDto(
                    sequence = 1,
                    geometry = "LINESTRING(129.075600 35.179600, 129.076000 35.179900)",
                ),
                RouteSegmentDto(
                    sequence = 2,
                    geometry = "POINT(129.076000 35.179900)",
                ),
                RouteSegmentDto(
                    sequence = 3,
                    geometry = "LINESTRING(129.076000 35.179900, 129.077000 35.180500)",
                ),
            ).toRoutePreviewModel(geometryParser = geometryParser)

        assertEquals(3, preview.segmentCount)
        assertEquals(2, preview.renderableSegmentCount)
        assertEquals(1, preview.fallbackSegmentCount)
        assertTrue(preview.hasRenderableLine)
        assertEquals(3, preview.polyline.points.size)
    }

    @Test
    fun `toRoutePreviewModel returns empty non renderable preview for empty segments`() {
        val preview = emptyList<RouteSegmentDto>().toRoutePreviewModel(geometryParser = geometryParser)

        assertEquals(0, preview.segmentCount)
        assertEquals(0, preview.renderableSegmentCount)
        assertEquals(0, preview.fallbackSegmentCount)
        assertFalse(preview.hasRenderableLine)
        assertTrue(preview.polyline.points.isEmpty())
    }

    @Test
    fun `toDomain exposes preview aggregation for later route preview consumers`() {
        val result =
            RouteSearchResponseDto(
                routes =
                    listOf(
                        RouteDto(
                            routeOption = "SAFE",
                            title = "Safe Route",
                            distanceMeter = 120,
                            estimatedTimeMinute = 2,
                            riskLevel = "LOW",
                            segments =
                                listOf(
                                    RouteSegmentDto(
                                        sequence = 1,
                                        geometry = "LINESTRING(129.075600 35.179600, 129.076000 35.179900)",
                                        distanceMeter = 120,
                                    ),
                                    RouteSegmentDto(
                                        sequence = 2,
                                        geometry = "LINESTRING(129.076000 35.179900)",
                                        distanceMeter = 0,
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
        assertEquals(2, route.preview.segmentCount)
        assertEquals(1, route.preview.renderableSegmentCount)
        assertEquals(1, route.preview.fallbackSegmentCount)
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
                            distanceMeter = 90,
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
