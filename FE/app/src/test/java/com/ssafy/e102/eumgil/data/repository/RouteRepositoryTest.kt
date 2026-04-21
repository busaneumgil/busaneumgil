package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteDefaults
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.RouteMockDataSource
import com.ssafy.e102.eumgil.data.route.RouteDto
import com.ssafy.e102.eumgil.data.route.RouteSearchResponseDto
import com.ssafy.e102.eumgil.data.route.RouteSegmentDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteRepositoryTest {
    @Test
    fun `searchRoutes returns fixture backed SAFE and SHORTEST routes and caches the result`() =
        runBlocking {
            val localDataSource = RouteLocalDataSource()
            val repository =
                DefaultRouteRepository(
                    localDataSource = localDataSource,
                    mockDataSource = RouteMockDataSource(),
                )
            val query = testRouteQuery()

            val result = repository.searchRoutes(query)
            val cachedResult = localDataSource.getCachedSearchResult(query)

            assertEquals(listOf(RouteOption.SAFE, RouteOption.SHORTEST), result.routes.map { route -> route.routeOption })
            assertTrue(result.routes.all { route -> route.previewPolyline.isRenderable })
            assertEquals(result, cachedResult)
        }

    @Test
    fun `searchRoutes normalizes invalid dto values while keeping valid route preview points`() =
        runBlocking {
            val repository =
                DefaultRouteRepository(
                    localDataSource = RouteLocalDataSource(),
                    mockDataSource =
                        RouteMockDataSource(
                            fixtureProvider = {
                                RouteSearchResponseDto(
                                    routes =
                                        listOf(
                                            RouteDto(
                                                routeOption = null,
                                                title = " ",
                                                distanceMeter = -1,
                                                estimatedTimeMinute = null,
                                                riskLevel = "unknown",
                                                segments =
                                                    listOf(
                                                        RouteSegmentDto(
                                                            sequence = -5,
                                                            geometry = "POINT(129.0756 35.1796)",
                                                            distanceMeter = -10,
                                                            hasStairs = true,
                                                            riskLevel = null,
                                                            guidanceMessage = " ",
                                                        ),
                                                        RouteSegmentDto(
                                                            sequence = 2,
                                                            geometry =
                                                                "LINESTRING(129.075600 35.179600, 129.076100 35.180000)",
                                                            distanceMeter = 120,
                                                            hasCrosswalk = true,
                                                            riskLevel = "LOW",
                                                            guidanceMessage = "Use the marked crosswalk.",
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

            assertEquals(RouteOption.SAFE, route.routeOption)
            assertEquals("Safe Route", route.title)
            assertEquals(120, route.summary.distanceMeters)
            assertEquals(2, route.summary.estimatedTimeMinutes)
            assertEquals(RouteRiskLevel.MEDIUM, route.summary.riskLevel)
            assertEquals(1, firstSegment.sequence)
            assertEquals(0, firstSegment.distanceMeters)
            assertEquals(RouteDefaults.DEFAULT_GUIDANCE_MESSAGE, firstSegment.guidanceMessage)
            assertTrue(firstSegment.polyline.points.isEmpty())
            assertEquals("Use the marked crosswalk.", secondSegment.guidanceMessage)
            assertTrue(route.previewPolyline.isRenderable)
            assertNotNull(route.previewPolyline.start)
            assertNotNull(route.previewPolyline.end)
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
