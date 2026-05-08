package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteDefaults
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchSourceType
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteTransportMode
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.RouteMockDataSource
import com.ssafy.e102.eumgil.data.mock.fixture.RouteFixtureSearchPayload
import com.ssafy.e102.eumgil.data.route.RouteAlertDto
import com.ssafy.e102.eumgil.data.route.RouteDto
import com.ssafy.e102.eumgil.data.route.RouteLegDto
import com.ssafy.e102.eumgil.data.route.RouteStepDto
import com.ssafy.e102.eumgil.data.route.RouteTransitStopDto
import com.ssafy.e102.eumgil.data.route.RouteSearchResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteRepositoryTest {
    @Test
    fun `getRouteSearchData returns fixture backed SAFE and SHORTEST routes and caches the result`() =
        runBlocking {
            val localDataSource = RouteLocalDataSource()
            val repository =
                DefaultRouteRepository(
                    localDataSource = localDataSource,
                    mockDataSource = RouteMockDataSource(),
                )
            val query = testRouteQuery()

            val searchData = repository.getRouteSearchData(query)
            val cachedSearchData = localDataSource.getCachedSearchData(query)
            val cachedRead = repository.getRouteSearchData(query)

            assertEquals(RouteSearchSourceType.MOCK_FIXTURE, searchData.source.type)
            assertEquals("busan-cityhall-to-station-demo", searchData.source.fixtureId)
            assertEquals("Busan City Hall to Busan Station demo route", searchData.source.label)
            assertEquals("rs_walk_busan_demo", searchData.result.searchId)
            assertEquals(listOf(RouteOption.SAFE, RouteOption.SHORTEST), searchData.result.availableOptions)
            assertEquals(
                listOf("walk_rt_safe_demo", "walk_rt_shortest_demo"),
                searchData.routes.map { route -> route.routeId },
            )
            assertTrue(searchData.routes.all { route -> route.transportMode == RouteTransportMode.WALK })
            assertTrue(searchData.routes.all { route -> route.legs.isNotEmpty() })
            assertTrue(searchData.routes.all { route -> route.previewPolyline.isRenderable })
            assertTrue(searchData.routes.all { route -> route.preview.segmentCount > 0 })
            assertEquals(searchData, cachedSearchData)
            assertTrue(searchData.primaryRoute?.hasRenderablePreview == true)
            assertTrue(cachedRead.source.isFromCache)
            assertEquals(searchData.result, cachedRead.result)
        }

    @Test
    fun `searchRoutes normalizes invalid dto values while keeping valid route preview points`() =
        runBlocking {
            val repository =
                DefaultRouteRepository(
                    localDataSource = RouteLocalDataSource(),
                    mockDataSource =
                        RouteMockDataSource(
                            fixturePayloadProvider = { request ->
                                RouteFixtureSearchPayload(
                                    fixtureId = "custom-invalid-dto-fixture",
                                    fixtureName = "Invalid DTO fixture",
                                    request = request,
                                    response =
                                        RouteSearchResponseDto(
                                            searchId = "rs_transit_invalid_fixture",
                                            routes =
                                                listOf(
                                                    RouteDto(
                                                        routeId = "pt_rt_invalid_fixture",
                                                        transportMode = "PUBLIC_TRANSIT",
                                                        routeOption = "RECOMMENDED",
                                                        title = " ",
                                                        distanceMeter = -1.0,
                                                        estimatedTimeMinute = null,
                                                        transferCount = 1,
                                                        badges = listOf("UNPAVED"),
                                                        legs =
                                                            listOf(
                                                                RouteLegDto(
                                                                    sequence = 1,
                                                                    type = "WALK",
                                                                    role = "WALK_TO_TRANSIT",
                                                                    instruction = " ",
                                                                    distanceMeter = -10.0,
                                                                    geometry = "POINT(129.0756 35.1796)",
                                                                    steps =
                                                                        listOf(
                                                                            RouteStepDto(
                                                                                sequence = -1,
                                                                                instruction = " ",
                                                                                distanceMeter = -10.0,
                                                                                geometry = "POINT(129.0756 35.1796)",
                                                                                alerts =
                                                                                    listOf(
                                                                                        RouteAlertDto(
                                                                                            type = "CURB",
                                                                                            distanceMeter = 10.0,
                                                                                        ),
                                                                                    ),
                                                                            ),
                                                                        ),
                                                                ),
                                                                RouteLegDto(
                                                                    sequence = 2,
                                                                    type = "BUS",
                                                                    role = "TRANSIT",
                                                                    instruction = "Take bus 100",
                                                                    estimatedTimeMinute = 7,
                                                                    routeNo = "100",
                                                                    boardingStop =
                                                                        RouteTransitStopDto(
                                                                            name = "Stop A",
                                                                            lat = 35.1796,
                                                                            lng = 129.0756,
                                                                        ),
                                                                    alightingStop =
                                                                        RouteTransitStopDto(
                                                                            name = "Stop B",
                                                                            lat = 35.1800,
                                                                            lng = 129.0761,
                                                                        ),
                                                                ),
                                                            ),
                                                    ),
                                                ),
                                        ),
                                )
                            },
                        ),
                )
            val query = testRouteQuery(requestedOptions = listOf(RouteOption.SAFE))

            val result = repository.searchRoutes(query)
            val route = result.routes.single()
            val firstSegment = route.segments.first()
            val secondSegment = route.segments.last()

            assertEquals("rs_transit_invalid_fixture", result.searchId)
            assertEquals("pt_rt_invalid_fixture", route.routeId)
            assertEquals(RouteTransportMode.PUBLIC_TRANSIT, route.transportMode)
            assertEquals(RouteOption.RECOMMENDED, route.routeOption)
            assertEquals("Recommended Route", route.title)
            assertEquals(0, route.summary.distanceMeters)
            assertEquals(0, route.summary.estimatedTimeMinutes)
            assertEquals(RouteRiskLevel.HIGH, route.summary.riskLevel)
            assertEquals(1, firstSegment.sequence)
            assertEquals(0, firstSegment.distanceMeters)
            assertEquals(RouteDefaults.DEFAULT_GUIDANCE_MESSAGE, firstSegment.guidanceMessage)
            assertTrue(firstSegment.polyline.points.isEmpty())
            assertEquals("Take bus 100", secondSegment.guidanceMessage)
            assertEquals(2, route.preview.segmentCount)
            assertEquals(0, route.preview.renderableSegmentCount)
            assertEquals(1, route.preview.fallbackSegmentCount)
            assertFalse(route.previewPolyline.isRenderable)
            assertTrue(route.hasFallbackSegments)
            assertEquals(route, result.findRoute(RouteOption.RECOMMENDED))
            assertEquals("100", route.legs.last().routeNo)
            assertNotNull(route.legs.last().boardingStop)
            assertNotNull(route.legs.last().alightingStop)
        }
}

private fun testRouteQuery(
    requestedOptions: List<RouteOption> = RouteOption.defaultSearchOptions,
): RouteSearchQuery =
    RouteSearchQuery(
        origin =
            RouteWaypoint(
                name = "Busan City Hall",
                coordinate = GeoCoordinate(latitude = 35.1796, longitude = 129.0756),
            ),
        destination =
            RouteWaypoint(
                name = "Busan Station",
                coordinate = GeoCoordinate(latitude = 35.1151, longitude = 129.0414),
            ),
        requestedOptions = requestedOptions,
    )
