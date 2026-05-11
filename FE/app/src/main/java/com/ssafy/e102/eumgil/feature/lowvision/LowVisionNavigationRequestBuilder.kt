package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RoutePolyline
import com.ssafy.e102.eumgil.core.model.RoutePreviewModel
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSearchResult
import com.ssafy.e102.eumgil.core.model.RouteSearchSource
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteSummary
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.core.model.toRouteWaypointOrNull
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import com.ssafy.e102.eumgil.data.remote.datasource.RouteApiException
import com.ssafy.e102.eumgil.feature.route.RouteNavigationRequest
import com.ssafy.e102.eumgil.feature.route.RouteNavigationSelectionHandoff
import kotlinx.coroutines.CancellationException

internal data class LowVisionNavigationPlan(
    val searchData: RouteSearchData,
    val selectedRoute: RouteCandidate,
)

internal suspend fun RouteRepository.buildLowVisionNavigationPlan(
    destinationSelectionRepository: DestinationSelectionRepository,
    origin: RouteWaypoint = LOW_VISION_DEFAULT_ORIGIN,
): LowVisionNavigationPlan? {
    val destination = destinationSelectionRepository.selectedDestination.value.toLowVisionRouteWaypoint()
    val walkQuery =
        RouteSearchQuery(
            origin = origin,
            destination = destination,
            requestedOptions = LOW_VISION_WALK_OPTIONS,
        )
    val walkSearchData =
        runCatching {
            getFreshLowVisionWalkRouteSearchData(query = walkQuery)
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            return fallbackLowVisionNavigationPlan(query = walkQuery)
        }
    val resolvedOrigin = walkSearchData.query.origin
    val selectedWalkRoute =
        walkSearchData.findRoute(RouteOption.SAFE)
            ?: walkSearchData.primaryRoute
            ?: walkSearchData.routes.firstOrNull()
            ?: return fallbackLowVisionNavigationPlan(query = walkSearchData.query)
    if (selectedWalkRoute.summary.distanceMeters <= LOW_VISION_TRANSIT_THRESHOLD_METERS) {
        return LowVisionNavigationPlan(
            searchData = walkSearchData,
            selectedRoute = selectedWalkRoute,
        )
    }

    val transitSearchData =
        runCatching {
            getFreshTransitRouteSearchData(
                RouteSearchQuery(
                    origin = resolvedOrigin,
                    destination = destination,
                    requestedOptions = LOW_VISION_TRANSIT_OPTIONS,
                ),
            )
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            return LowVisionNavigationPlan(
                searchData = walkSearchData,
                selectedRoute = selectedWalkRoute,
            )
        }
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
    origin: RouteWaypoint = LOW_VISION_DEFAULT_ORIGIN,
): RouteNavigationRequest? {
    return try {
        val plan =
            buildLowVisionNavigationPlan(
                destinationSelectionRepository = destinationSelectionRepository,
                origin = origin,
            ) ?: return null
        val searchId = plan.searchData.searchId?.takeIf(String::isNotBlank)
        val routeId =
            plan.selectedRoute.serverRouteId?.takeIf(String::isNotBlank)
                ?: plan.selectedRoute.routeId.takeIf(String::isNotBlank)
        val selectionHandoff =
            if (searchId != null && routeId != null) {
                selectLowVisionRouteOrNull(
                    routeId = routeId,
                    searchId = searchId,
                    selectedRoute = plan.selectedRoute,
                )
            } else {
                null
            }
        RouteNavigationRequest(
            origin = plan.searchData.result.origin,
            destination = plan.searchData.result.destination,
            selectedRoute = plan.selectedRoute,
            source = plan.searchData.source,
            selectionHandoff = selectionHandoff,
        )
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        null
    }
}

private suspend fun RouteRepository.selectLowVisionRouteOrNull(
    routeId: String,
    searchId: String,
    selectedRoute: RouteCandidate,
): RouteNavigationSelectionHandoff? =
    try {
        val sessionData =
            selectRoute(
                routeId = routeId,
                searchId = searchId,
            )
        RouteNavigationSelectionHandoff(
            searchId = searchId,
            routeId = routeId,
            sessionId = sessionData.sessionId,
            initialRemainingDistanceMeters =
                sessionData.totalDistanceMeters ?: selectedRoute.summary.distanceMeters,
            initialRemainingDurationSeconds =
                sessionData.totalDurationSeconds
                    ?: selectedRoute.summary.durationSeconds
                    ?: selectedRoute.summary.estimatedTimeMinutes * SECONDS_PER_MINUTE,
        )
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        null
    }

private suspend fun RouteRepository.getFreshLowVisionWalkRouteSearchData(query: RouteSearchQuery): RouteSearchData =
    try {
        getFreshRouteSearchData(query)
    } catch (throwable: Throwable) {
        if (throwable is CancellationException ||
            throwable !is RouteApiException ||
            query.origin.isLowVisionDefaultOrigin()
        ) {
            throw throwable
        }
        getFreshRouteSearchData(query.copy(origin = LOW_VISION_DEFAULT_ORIGIN))
    }

private fun fallbackLowVisionNavigationPlan(query: RouteSearchQuery): LowVisionNavigationPlan {
    val fallbackRoute = query.toLowVisionFallbackRoute()
    return LowVisionNavigationPlan(
        searchData =
            RouteSearchData(
                query = query,
                result =
                    RouteSearchResult(
                        origin = query.origin,
                        destination = query.destination,
                        routes = listOf(fallbackRoute),
                    ),
                source = RouteSearchSource.serverApi(label = LOW_VISION_FALLBACK_SOURCE_LABEL),
            ),
        selectedRoute = fallbackRoute,
    )
}

private fun RouteSearchQuery.toLowVisionFallbackRoute(): RouteCandidate {
    val path = origin.coordinate.toFallbackPath(destination.coordinate)
    val routePolyline = RoutePolyline(points = path)
    val segmentDistance = LOW_VISION_FALLBACK_DISTANCE_METERS / LOW_VISION_FALLBACK_SEGMENT_COUNT
    val segments =
        listOf(
            fallbackSegment(
                sequence = 1,
                points = listOf(path[0], path[1]),
                distanceMeters = segmentDistance,
                guidanceMessage = "Continue straight from the start.",
            ),
            fallbackSegment(
                sequence = 2,
                points = listOf(path[1], path[2]),
                distanceMeters = segmentDistance,
                guidanceMessage = "Continue carefully and check nearby crossings.",
            ),
            fallbackSegment(
                sequence = 3,
                points = listOf(path[2], path[3]),
                distanceMeters =
                    LOW_VISION_FALLBACK_DISTANCE_METERS -
                        segmentDistance * (LOW_VISION_FALLBACK_SEGMENT_COUNT - 1),
                guidanceMessage = "Continue toward the destination.",
            ),
        )
    return RouteCandidate(
        routeId = LOW_VISION_FALLBACK_ROUTE_ID,
        routeOption = RouteOption.SAFE,
        title = LOW_VISION_FALLBACK_ROUTE_TITLE,
        summary =
            RouteSummary(
                distanceMeters = LOW_VISION_FALLBACK_DISTANCE_METERS,
                estimatedTimeMinutes = LOW_VISION_FALLBACK_DURATION_SECONDS / SECONDS_PER_MINUTE,
                riskLevel = RouteRiskLevel.LOW,
                durationSeconds = LOW_VISION_FALLBACK_DURATION_SECONDS,
            ),
        geometry = routePolyline,
        preview =
            RoutePreviewModel(
                polyline = routePolyline,
                segmentCount = segments.size,
                renderableSegmentCount = segments.count(RouteSegment::hasRenderablePolyline),
            ),
        segments = segments,
    )
}

private fun fallbackSegment(
    sequence: Int,
    points: List<GeoCoordinate>,
    distanceMeters: Int,
    guidanceMessage: String,
): RouteSegment =
    RouteSegment(
        sequence = sequence,
        polyline = RoutePolyline(points = points),
        distanceMeters = distanceMeters,
        riskLevel = RouteRiskLevel.LOW,
        guidanceMessage = guidanceMessage,
    )

private fun GeoCoordinate.toFallbackPath(destination: GeoCoordinate): List<GeoCoordinate> =
    listOf(
        this,
        interpolateTo(destination, fraction = 0.33),
        interpolateTo(destination, fraction = 0.66),
        destination,
    )

private fun GeoCoordinate.interpolateTo(
    destination: GeoCoordinate,
    fraction: Double,
): GeoCoordinate =
    GeoCoordinate(
        latitude = latitude + (destination.latitude - latitude) * fraction,
        longitude = longitude + (destination.longitude - longitude) * fraction,
    )

private fun RouteWaypoint.isLowVisionDefaultOrigin(): Boolean =
    coordinate == LOW_VISION_DEFAULT_ORIGIN.coordinate

private fun PlaceDestination?.toLowVisionRouteWaypoint(): RouteWaypoint =
    this?.toRouteWaypointOrNull() ?: LOW_VISION_DEFAULT_DESTINATION

internal fun LocationSnapshot?.toLowVisionRouteOriginWaypoint(): RouteWaypoint =
    this?.let { snapshot ->
        RouteWaypoint(
            name = "\uD604\uC7AC \uC704\uCE58",
            address = "\uD604\uC7AC \uC704\uCE58",
            coordinate = GeoCoordinate(latitude = snapshot.latitude, longitude = snapshot.longitude),
        )
    } ?: LOW_VISION_DEFAULT_ORIGIN

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
private const val LOW_VISION_FALLBACK_DISTANCE_METERS = 600
private const val LOW_VISION_FALLBACK_DURATION_SECONDS = 600
private const val LOW_VISION_FALLBACK_SEGMENT_COUNT = 3
private const val LOW_VISION_FALLBACK_ROUTE_ID = "low-vision-fallback-route"
private const val LOW_VISION_FALLBACK_ROUTE_TITLE = "Low vision route"
private const val LOW_VISION_FALLBACK_SOURCE_LABEL = "Low vision fallback route"
private const val SECONDS_PER_MINUTE = 60
private val LOW_VISION_WALK_OPTIONS = listOf(RouteOption.SAFE, RouteOption.SHORTEST)
private val LOW_VISION_TRANSIT_OPTIONS =
    listOf(
        RouteOption.RECOMMENDED,
        RouteOption.MIN_TRANSFER,
        RouteOption.MIN_WALK,
    )
