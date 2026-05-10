package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.AuthGateState
import com.ssafy.e102.eumgil.core.model.AuthSession
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteDefaults
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchSourceType
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteTransportMode
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
import com.ssafy.e102.eumgil.data.mock.fixture.MockRouteFixtures
import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.datasource.AuthRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.RouteApiException
import com.ssafy.e102.eumgil.data.remote.datasource.RouteRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.ReissueResponseDto
import com.ssafy.e102.eumgil.data.route.RouteAlertDto
import com.ssafy.e102.eumgil.data.route.RouteDto
import com.ssafy.e102.eumgil.data.route.RouteLegDto
import com.ssafy.e102.eumgil.data.route.RouteSearchRequestDto
import com.ssafy.e102.eumgil.data.route.RouteSearchResponseDto
import com.ssafy.e102.eumgil.data.route.RouteStepDto
import com.ssafy.e102.eumgil.data.route.RouteTransitStopDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteRepositoryTest {
    @Test
    fun `getRouteSearchData returns server backed SAFE and SHORTEST routes and caches the result`() =
        runBlocking {
            val localDataSource = RouteLocalDataSource()
            var remoteCallCount = 0
            val repository =
                DefaultRouteRepository(
                    localDataSource = localDataSource,
                    remoteDataSource =
                        remoteDataSource { request ->
                            remoteCallCount += 1
                            MockRouteFixtures.searchRoutes(request)
                        },
                )
            val query = testRouteQuery()

            val searchData = repository.getRouteSearchData(query)
            val cachedSearchData = localDataSource.getCachedSearchData(query)
            val cachedRead = repository.getRouteSearchData(query)

            assertEquals(RouteSearchSourceType.SERVER_API, searchData.source.type)
            assertEquals("실시간 경로", searchData.source.label)
            assertFalse(searchData.source.isFromCache)
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
            assertEquals(1, remoteCallCount)
        }

    @Test
    fun `searchRoutes normalizes invalid dto values while keeping valid route preview points`() =
        runBlocking {
            val repository =
                DefaultRouteRepository(
                    localDataSource = RouteLocalDataSource(),
                    remoteDataSource =
                        remoteDataSource {
                            RouteSearchResponseDto(
                                searchId = "rs_transit_invalid_server_payload",
                                routes =
                                    listOf(
                                        RouteDto(
                                            routeId = "pt_rt_invalid_server_payload",
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
                            )
                        },
                )
            val query = testRouteQuery(requestedOptions = listOf(RouteOption.SAFE))

            val result = repository.searchRoutes(query)
            val route = result.routes.single()
            val firstSegment = route.segments.first()
            val secondSegment = route.segments.last()

            assertEquals("rs_transit_invalid_server_payload", result.searchId)
            assertEquals("pt_rt_invalid_server_payload", route.routeId)
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

    @Test
    fun `getRouteSearchData propagates remote failure without mock fallback or cache write`() =
        runBlocking {
            val localDataSource = RouteLocalDataSource()
            val repository =
                DefaultRouteRepository(
                    localDataSource = localDataSource,
                    remoteDataSource =
                        remoteDataSource {
                            throw RouteApiException(
                                httpStatusCode = 404,
                                status = "RT4040",
                                message = "탐색 가능한 경로가 없습니다.",
                            )
                        },
                )
            val query = testRouteQuery()

            var failure: Throwable? = null
            try {
                repository.getRouteSearchData(query)
            } catch (throwable: Throwable) {
                failure = throwable
            }

            val error = failure as? RouteApiException
            assertNotNull(error)
            assertEquals(404, error?.httpStatusCode)
            assertEquals("RT4040", error?.status)
            assertEquals("탐색 가능한 경로가 없습니다.", error?.message)
            assertNull(localDataSource.getCachedSearchData(query))
        }
    @Test
    fun `getRouteSearchData retries with refreshed auth session when remote responds unauthorized`() =
        runBlocking {
            val localDataSource = RouteLocalDataSource()
            val authSessionRepository =
                TestAuthSessionRepository(
                    initialState =
                        AuthGateState(
                            authSession = AuthSession(accessToken = "expired-token", refreshToken = "refresh-token"),
                            isProfileCompleted = true,
                        ),
                )
            var requestCount = 0
            val repository =
                DefaultRouteRepository(
                    localDataSource = localDataSource,
                    remoteDataSource =
                        remoteDataSource { request ->
                            requestCount += 1
                            when (requestCount) {
                                1 ->
                                    throw RouteApiException(
                                        httpStatusCode = 401,
                                        status = "AUTH_401",
                                        message = "인증이 필요합니다.",
                                    )

                                2 -> {
                                    assertEquals(
                                        "refreshed-access-token",
                                        authSessionRepository.getAuthGateState().authSession?.accessToken,
                                    )
                                    MockRouteFixtures.searchRoutes(request)
                                }

                                else -> error("Unexpected route retry count: $requestCount")
                            }
                        },
                    authSessionRepository = authSessionRepository,
                    authRemoteDataSource =
                        object : AuthRemoteDataSource(HttpJsonClient(baseUrl = "https://example.com")) {
                            override suspend fun reissue(refreshToken: String): ReissueResponseDto {
                                assertEquals("refresh-token", refreshToken)
                                return ReissueResponseDto(
                                    accessToken = "refreshed-access-token",
                                    refreshToken = "refreshed-refresh-token",
                                )
                            }
                        },
                )
            val query = testRouteQuery()

            val searchData = repository.getRouteSearchData(query)

            assertEquals("rs_walk_busan_demo", searchData.result.searchId)
            assertEquals(2, requestCount)
            assertEquals(
                "refreshed-refresh-token",
                authSessionRepository.getAuthGateState().authSession?.refreshToken,
            )
            assertEquals(searchData, localDataSource.getCachedSearchData(query))
        }

    @Test
    fun `getRouteSearchData clears auth session and propagates auth failure when token refresh fails`() =
        runBlocking {
            val localDataSource = RouteLocalDataSource()
            val authSessionRepository =
                TestAuthSessionRepository(
                    initialState =
                        AuthGateState(
                            authSession = AuthSession(accessToken = "expired-token", refreshToken = "refresh-token"),
                            isProfileCompleted = true,
                        ),
                )
            val repository =
                DefaultRouteRepository(
                    localDataSource = localDataSource,
                    remoteDataSource =
                        remoteDataSource {
                            throw RouteApiException(
                                httpStatusCode = 403,
                                status = "AUTH_403",
                                message = "인증이 필요합니다.",
                            )
                        },
                    authSessionRepository = authSessionRepository,
                    authRemoteDataSource =
                        object : AuthRemoteDataSource(HttpJsonClient(baseUrl = "https://example.com")) {
                            override suspend fun reissue(refreshToken: String): ReissueResponseDto {
                                throw IllegalStateException("refresh failed")
                            }
                        },
                )
            val query = testRouteQuery()

            val failure = runCatching { repository.getRouteSearchData(query) }.exceptionOrNull() as? RouteApiException

            requireNotNull(failure)
            assertEquals(401, failure.httpStatusCode)
            assertEquals("ROUTE_AUTHENTICATION_FAILED", failure.status)
            assertEquals("인증이 필요합니다.", failure.message)
            assertNull(authSessionRepository.getAuthGateState().authSession)
            assertNull(localDataSource.getCachedSearchData(query))
        }
}

private fun remoteDataSource(
    responseProvider: suspend (RouteSearchRequestDto) -> RouteSearchResponseDto,
): RouteRemoteDataSource =
    object : RouteRemoteDataSource(
        postRequestExecutor = { _, _, _ ->
            error("repository tests override searchWalkRoutes directly")
        },
    ) {
        override suspend fun searchWalkRoutes(request: RouteSearchRequestDto): RouteSearchResponseDto =
            responseProvider(request)
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
