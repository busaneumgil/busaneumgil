package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.core.model.toRouteWaypointOrNull
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import com.ssafy.e102.eumgil.feature.route.RouteNavigationRequest
import com.ssafy.e102.eumgil.feature.route.RouteNavigationSelectionHandoff
import kotlinx.coroutines.CancellationException

internal data class LowVisionNavigationPlan(
    val searchData: RouteSearchData,
    val selectedRoute: RouteCandidate,
)

internal suspend fun RouteRepository.buildLowVisionNavigationPlan(
    destinationSelectionRepository: DestinationSelectionRepository,
): LowVisionNavigationPlan? {
    val destination = destinationSelectionRepository.selectedDestination.value.toLowVisionRouteWaypoint()
    val walkSearchData =
        getRouteSearchData(
            RouteSearchQuery(
                origin = LOW_VISION_DEFAULT_ORIGIN,
                destination = destination,
                requestedOptions = LOW_VISION_WALK_OPTIONS,
            ),
        )
    val selectedWalkRoute =
        walkSearchData.findRoute(RouteOption.SAFE)
            ?: walkSearchData.primaryRoute
            ?: walkSearchData.routes.firstOrNull()
            ?: return null
    if (selectedWalkRoute.summary.distanceMeters <= LOW_VISION_TRANSIT_THRESHOLD_METERS) {
        return LowVisionNavigationPlan(
            searchData = walkSearchData,
            selectedRoute = selectedWalkRoute,
        )
    }

    val transitSearchData =
        getTransitRouteSearchData(
            RouteSearchQuery(
                origin = LOW_VISION_DEFAULT_ORIGIN,
                destination = destination,
                requestedOptions = LOW_VISION_TRANSIT_OPTIONS,
            ),
        )
    val selectedTransitRoute =
        transitSearchData.findRoute(RouteOption.RECOMMENDED)
            ?: transitSearchData.primaryRoute
            ?: transitSearchData.routes.firstOrNull()
            ?: return LowVisionNavigationPlan(
                searchData = walkSearchData,
                selectedRoute = selectedWalkRoute,
            )

    return LowVisionNavigationPlan(
        searchData = transitSearchData,
        selectedRoute = selectedTransitRoute,
    )
}

internal suspend fun RouteRepository.buildLowVisionNavigationRequest(
    destinationSelectionRepository: DestinationSelectionRepository,
): RouteNavigationRequest? {
    return try {
        val plan = buildLowVisionNavigationPlan(destinationSelectionRepository) ?: return null
        val searchId = plan.searchData.searchId?.takeIf(String::isNotBlank) ?: return null
        val routeId = plan.selectedRoute.serverRouteId?.takeIf(String::isNotBlank) ?: return null
        val sessionData =
            selectRoute(
                routeId = routeId,
                searchId = searchId,
            )
        RouteNavigationRequest(
            origin = plan.searchData.result.origin,
            destination = plan.searchData.result.destination,
            selectedRoute = plan.selectedRoute,
            source = plan.searchData.source,
            selectionHandoff =
                RouteNavigationSelectionHandoff(
                    searchId = searchId,
                    routeId = routeId,
                    sessionId = sessionData.sessionId,
                    initialRemainingDistanceMeters = sessionData.remainingDistanceMeters,
                    initialRemainingDurationSeconds = sessionData.remainingDurationSeconds,
                ),
        )
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        null
    }
}

private fun PlaceDestination?.toLowVisionRouteWaypoint(): RouteWaypoint =
    this?.toRouteWaypointOrNull() ?: LOW_VISION_DEFAULT_DESTINATION

private val LOW_VISION_DEFAULT_ORIGIN =
    RouteWaypoint(
        name = "현재 위치",
        address = "기본 출발지",
        coordinate = GeoCoordinate(latitude = 35.1796, longitude = 129.0756),
    )

private val LOW_VISION_DEFAULT_DESTINATION =
    RouteWaypoint(
        name = "부산역",
        address = "부산 동구 중앙대로 206",
        coordinate = GeoCoordinate(latitude = 35.1151, longitude = 129.0414),
    )

private const val LOW_VISION_TRANSIT_THRESHOLD_METERS = 750
private val LOW_VISION_WALK_OPTIONS = listOf(RouteOption.SAFE, RouteOption.SHORTEST)
private val LOW_VISION_TRANSIT_OPTIONS =
    listOf(
        RouteOption.RECOMMENDED,
        RouteOption.MIN_TRANSFER,
        RouteOption.MIN_WALK,
    )
