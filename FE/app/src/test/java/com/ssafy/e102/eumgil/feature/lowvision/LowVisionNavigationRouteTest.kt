package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteRatingData
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import com.ssafy.e102.eumgil.data.repository.RouteRerouteData
import com.ssafy.e102.eumgil.data.repository.RouteSessionData
import com.ssafy.e102.eumgil.data.repository.RouteTransitRefreshData
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
