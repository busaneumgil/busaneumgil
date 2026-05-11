package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSearchResult
import com.ssafy.e102.eumgil.core.model.RouteSearchSource
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSummary
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteRatingData
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import com.ssafy.e102.eumgil.data.repository.RouteRerouteData
import com.ssafy.e102.eumgil.data.repository.RouteSessionData
import com.ssafy.e102.eumgil.data.repository.RouteTransitRefreshData
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class LowVisionNavigationRouteTest {
    @Test
    fun `low vision navigation treats exit events as home navigation`() {
        assertEquals(true, shouldNavigateLowVisionHome(NavigationUiEvent.NavigateToMap))
        assertEquals(true, shouldNavigateLowVisionHome(NavigationUiEvent.NavigateToArrival))
    }

    @Test
    fun `low vision navigation ignores non exit navigation events for home`() {
        assertEquals(false, shouldNavigateLowVisionHome(NavigationUiEvent.NavigateBack))
        assertEquals(false, shouldNavigateLowVisionHome(NavigationUiEvent.NavigateToSavedRoute))
        assertEquals(false, shouldNavigateLowVisionHome(NavigationUiEvent.NavigateToRouteDetail(RouteOption.SAFE)))
    }

    @Test
    fun `low vision navigation request returns null instead of crashing when route api fails`() =
        runBlocking {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            destinationSelectionRepository.updateSelectedDestination(
                PlaceDestination(
                    placeId = "real-place-id",
                    name = "Real Place",
                    address = "Busan",
                    latitude = 35.2,
                    longitude = 129.2,
                ),
            )

            val request =
                ThrowingRouteRepository()
                    .buildLowVisionNavigationRequest(destinationSelectionRepository)

            assertNull(request)
        }

    @Test
    fun `low vision navigation request fetches a fresh route search before selecting`() =
        runBlocking {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            destinationSelectionRepository.updateSelectedDestination(
                PlaceDestination(
                    placeId = "real-place-id",
                    name = "Real Place",
                    address = "Busan",
                    latitude = 35.2,
                    longitude = 129.2,
                ),
            )
            val routeRepository = FreshRouteSearchTrackingRepository()

            val request = routeRepository.buildLowVisionNavigationRequest(destinationSelectionRepository)

            assertEquals("fresh-search", request?.selectionHandoff?.searchId)
            assertEquals("fresh-route", request?.selectionHandoff?.routeId)
            assertEquals(120, request?.selectionHandoff?.initialRemainingDistanceMeters)
            assertEquals(120, request?.selectionHandoff?.initialRemainingDurationSeconds)
            assertTrue(routeRepository.freshWalkSearchCalled)
        }

    @Test
    fun `low vision navigation request can select route id when server route id is missing`() =
        runBlocking {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            destinationSelectionRepository.updateSelectedDestination(
                PlaceDestination(
                    placeId = "real-place-id",
                    name = "Real Place",
                    address = "Busan",
                    latitude = 35.2,
                    longitude = 129.2,
                ),
            )
            val routeRepository = RouteIdOnlyRouteSearchRepository()

            val request = routeRepository.buildLowVisionNavigationRequest(destinationSelectionRepository)

            assertEquals("fresh-route", request?.selectionHandoff?.routeId)
            assertEquals("session-1", request?.selectionHandoff?.sessionId)
        }

    @Test
    fun `low vision navigation falls back to walk route when transit search fails`() =
        runBlocking {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            destinationSelectionRepository.updateSelectedDestination(
                PlaceDestination(
                    placeId = "far-place-id",
                    name = "Far Place",
                    address = "Busan",
                    latitude = 35.22,
                    longitude = 129.09,
                ),
            )
            val routeRepository = TransitFailureFallbackRouteRepository()

            val request = routeRepository.buildLowVisionNavigationRequest(destinationSelectionRepository)

            assertEquals("walk-route", request?.selectionHandoff?.routeId)
            assertEquals("fresh-walk-search", request?.selectionHandoff?.searchId)
            assertTrue(routeRepository.freshTransitSearchCalled)
        }
}

private class ThrowingRouteRepository : RouteRepository {
    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        throw IllegalStateException("route api failed")

    override suspend fun getTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        throw IllegalStateException("transit route api failed")

    override suspend fun selectRoute(
        routeId: String,
        searchId: String,
    ): RouteSessionData = throw IllegalStateException("select route failed")

    override suspend fun refreshTransit(
        routeId: String,
        legSequence: Int,
    ): RouteTransitRefreshData = throw IllegalStateException("refresh failed")

    override suspend fun reroute(
        routeId: String,
        currentPoint: com.ssafy.e102.eumgil.core.model.GeoCoordinate,
    ): RouteRerouteData = throw IllegalStateException("reroute failed")

    override suspend fun endRoute(routeId: String): RouteSessionData =
        throw IllegalStateException("end route failed")

    override suspend fun rateRoute(
        sessionId: String,
        score: Int,
    ): RouteRatingData = throw IllegalStateException("rating failed")
}

private class FreshRouteSearchTrackingRepository : RouteRepository {
    var freshWalkSearchCalled: Boolean = false

    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        error("cached route search should not be used for low vision navigation start")

    override suspend fun getFreshRouteSearchData(query: RouteSearchQuery): RouteSearchData {
        freshWalkSearchCalled = true
        return lowVisionRouteSearchData(
            query = query,
            searchId = "fresh-search",
            routeId = "fresh-route",
            distanceMeters = 120.0,
        )
    }

    override suspend fun getTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        error("transit route search was not expected")

    override suspend fun selectRoute(
        routeId: String,
        searchId: String,
    ): RouteSessionData {
        assertEquals("fresh-route", routeId)
        assertEquals("fresh-search", searchId)
        return RouteSessionData(sessionId = "session-1")
    }

    override suspend fun refreshTransit(
        routeId: String,
        legSequence: Int,
    ): RouteTransitRefreshData = throw IllegalStateException("refresh failed")

    override suspend fun reroute(
        routeId: String,
        currentPoint: com.ssafy.e102.eumgil.core.model.GeoCoordinate,
    ): RouteRerouteData = throw IllegalStateException("reroute failed")

    override suspend fun endRoute(routeId: String): RouteSessionData =
        throw IllegalStateException("end route failed")

    override suspend fun rateRoute(
        sessionId: String,
        score: Int,
    ): RouteRatingData = throw IllegalStateException("rating failed")
}

private class RouteIdOnlyRouteSearchRepository : RouteRepository {
    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        error("cached route search should not be used for low vision navigation start")

    override suspend fun getFreshRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        lowVisionRouteSearchData(
            query = query,
            searchId = "fresh-search",
            routeId = "fresh-route",
            serverRouteId = null,
            distanceMeters = 120.0,
        )

    override suspend fun getTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        error("transit route search was not expected")

    override suspend fun selectRoute(
        routeId: String,
        searchId: String,
    ): RouteSessionData {
        assertEquals("fresh-route", routeId)
        assertEquals("fresh-search", searchId)
        return RouteSessionData(sessionId = "session-1")
    }

    override suspend fun refreshTransit(
        routeId: String,
        legSequence: Int,
    ): RouteTransitRefreshData = throw IllegalStateException("refresh failed")

    override suspend fun reroute(
        routeId: String,
        currentPoint: com.ssafy.e102.eumgil.core.model.GeoCoordinate,
    ): RouteRerouteData = throw IllegalStateException("reroute failed")

    override suspend fun endRoute(routeId: String): RouteSessionData =
        throw IllegalStateException("end route failed")

    override suspend fun rateRoute(
        sessionId: String,
        score: Int,
    ): RouteRatingData = throw IllegalStateException("rating failed")
}

private class TransitFailureFallbackRouteRepository : RouteRepository {
    var freshTransitSearchCalled: Boolean = false

    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        error("cached route search should not be used for low vision navigation start")

    override suspend fun getFreshRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        lowVisionRouteSearchData(
            query = query,
            searchId = "fresh-walk-search",
            routeId = "walk-route",
            distanceMeters = 2_400.0,
        )

    override suspend fun getTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        error("cached transit route search should not be used")

    override suspend fun getFreshTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData {
        freshTransitSearchCalled = true
        throw IllegalStateException("transit route api failed")
    }

    override suspend fun selectRoute(
        routeId: String,
        searchId: String,
    ): RouteSessionData {
        assertEquals("walk-route", routeId)
        assertEquals("fresh-walk-search", searchId)
        return RouteSessionData(sessionId = "session-walk")
    }

    override suspend fun refreshTransit(
        routeId: String,
        legSequence: Int,
    ): RouteTransitRefreshData = throw IllegalStateException("refresh failed")

    override suspend fun reroute(
        routeId: String,
        currentPoint: com.ssafy.e102.eumgil.core.model.GeoCoordinate,
    ): RouteRerouteData = throw IllegalStateException("reroute failed")

    override suspend fun endRoute(routeId: String): RouteSessionData =
        throw IllegalStateException("end route failed")

    override suspend fun rateRoute(
        sessionId: String,
        score: Int,
    ): RouteRatingData = throw IllegalStateException("rating failed")
}

private fun lowVisionRouteSearchData(
    query: RouteSearchQuery,
    searchId: String,
    routeId: String,
    serverRouteId: String? = routeId,
    distanceMeters: Double,
): RouteSearchData =
    RouteSearchData(
        query = query,
        result =
            RouteSearchResult(
                origin = query.origin,
                destination = query.destination,
                searchId = searchId,
                routes =
                    listOf(
                        RouteCandidate(
                            routeId = routeId,
                            serverRouteId = serverRouteId,
                            routeOption = RouteOption.SAFE,
                            title = "Fresh route",
                            summary =
                                RouteSummary(
                                    distanceMeters = distanceMeters.toInt(),
                                    estimatedTimeMinutes = 2,
                                    riskLevel = RouteRiskLevel.LOW,
                                ),
                        ),
                    ),
            ),
        source = RouteSearchSource.serverApi(),
    )
