package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSearchResult
import com.ssafy.e102.eumgil.core.model.RouteSearchSource
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.AuthRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.RouteApiException
import com.ssafy.e102.eumgil.data.remote.datasource.RouteRemoteDataSource
import com.ssafy.e102.eumgil.data.route.DefaultRouteGeometryParser
import com.ssafy.e102.eumgil.data.route.RouteGeometryParser
import com.ssafy.e102.eumgil.data.route.RoutePointDto
import com.ssafy.e102.eumgil.data.route.RouteRatingRequestDto
import com.ssafy.e102.eumgil.data.route.RouteRatingResponseDto
import com.ssafy.e102.eumgil.data.route.RouteRerouteRequestDto
import com.ssafy.e102.eumgil.data.route.RouteRerouteResponseDto
import com.ssafy.e102.eumgil.data.route.RouteSearchRequestDto
import com.ssafy.e102.eumgil.data.route.RouteSearchResponseDto
import com.ssafy.e102.eumgil.data.route.RouteSelectRequestDto
import com.ssafy.e102.eumgil.data.route.RouteSessionResponseDto
import com.ssafy.e102.eumgil.data.route.RouteTransitArrivalDto
import com.ssafy.e102.eumgil.data.route.RouteTransitRefreshRequestDto
import com.ssafy.e102.eumgil.data.route.RouteTransitRefreshResponseDto
import com.ssafy.e102.eumgil.data.route.toDomain
import com.ssafy.e102.eumgil.data.route.toRequestDto
import com.ssafy.e102.eumgil.data.route.toRouteCandidate

interface RouteRepository {
    // Primary read-model entry point for 199 route setting and 200/201/202 handoff consumers.
    suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData

    suspend fun getFreshRouteSearchData(query: RouteSearchQuery): RouteSearchData = getRouteSearchData(query)

    suspend fun searchRoutes(query: RouteSearchQuery): RouteSearchResult = getRouteSearchData(query).result

    suspend fun getTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData

    suspend fun getFreshTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        getTransitRouteSearchData(query)

    suspend fun searchTransitRoutes(query: RouteSearchQuery): RouteSearchResult = getTransitRouteSearchData(query).result

    suspend fun selectRoute(
        routeId: String,
        searchId: String,
    ): RouteSessionData

    suspend fun refreshTransit(
        routeId: String,
        legSequence: Int,
    ): RouteTransitRefreshData

    suspend fun reroute(
        routeId: String,
        currentPoint: GeoCoordinate,
    ): RouteRerouteData

    suspend fun endRoute(routeId: String): RouteSessionData

    suspend fun rateRoute(
        sessionId: String,
        score: Int,
    ): RouteRatingData
}

class DefaultRouteRepository(
    private val localDataSource: RouteLocalDataSource,
    private val remoteDataSource: RouteRemoteDataSource,
    private val geometryParser: RouteGeometryParser = DefaultRouteGeometryParser(),
    authSessionRepository: AuthSessionRepository? = null,
    authRemoteDataSource: AuthRemoteDataSource? = null,
) : RouteRepository {
    private val authenticatedRequestRunner =
        if (authSessionRepository != null && authRemoteDataSource != null) {
            AuthenticatedRequestRunner(
                authSessionRepository = authSessionRepository,
                authRemoteDataSource = authRemoteDataSource,
            )
        } else {
            null
        }

    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        getSearchData(query = query, useCache = true) { request ->
            remoteDataSource.searchWalkRoutes(request)
        }

    override suspend fun getFreshRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        getSearchData(query = query, useCache = false) { request ->
            remoteDataSource.searchWalkRoutes(request)
        }

    override suspend fun getTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        getSearchData(query = query, useCache = true) { request ->
            remoteDataSource.searchTransitRoutes(request)
        }

    override suspend fun getFreshTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        getSearchData(query = query, useCache = false) { request ->
            remoteDataSource.searchTransitRoutes(request)
        }

    override suspend fun selectRoute(
        routeId: String,
        searchId: String,
    ): RouteSessionData =
        runAuthenticatedRemoteRequest {
            remoteDataSource.selectRoute(
                routeId = routeId,
                request = RouteSelectRequestDto(searchId = searchId),
            )
        }.toRepositoryData()

    override suspend fun refreshTransit(
        routeId: String,
        legSequence: Int,
    ): RouteTransitRefreshData =
        runAuthenticatedRemoteRequest {
            remoteDataSource.refreshTransit(
                routeId = routeId,
                request = RouteTransitRefreshRequestDto(legSequence = legSequence),
            )
        }.toRepositoryData()

    override suspend fun reroute(
        routeId: String,
        currentPoint: GeoCoordinate,
    ): RouteRerouteData =
        runAuthenticatedRemoteRequest {
            remoteDataSource.reroute(
                RouteRerouteRequestDto(
                    routeId = routeId,
                    currentPoint = currentPoint.toPointDto(),
                ),
            )
        }.toRepositoryData(geometryParser = geometryParser)

    override suspend fun endRoute(routeId: String): RouteSessionData =
        runAuthenticatedRemoteRequest {
            remoteDataSource.endRoute(routeId = routeId)
        }.toRepositoryData()

    override suspend fun rateRoute(
        sessionId: String,
        score: Int,
    ): RouteRatingData =
        runAuthenticatedRemoteRequest {
            remoteDataSource.rateRoute(
                RouteRatingRequestDto(
                    sessionId = sessionId,
                    score = score,
                ),
            )
        }.toRepositoryData()

    private suspend fun getSearchData(
        query: RouteSearchQuery,
        useCache: Boolean,
        remoteSearch: suspend (RouteSearchRequestDto) -> RouteSearchResponseDto,
    ): RouteSearchData {
        if (useCache) {
            localDataSource.getCachedSearchData(query)?.let { cachedSearchData ->
                return cachedSearchData.copy(source = cachedSearchData.source.asCached())
            }
        }

        val response = runAuthenticatedRemoteRequest { remoteSearch(query.toRequestDto()) }
        val result = response.toDomain(query = query, geometryParser = geometryParser)
        val searchData =
            RouteSearchData(
                query = query,
                result = result,
                source = RouteSearchSource.serverApi(),
            )
        localDataSource.updateCachedSearchData(query = query, searchData = searchData)
        return searchData
    }

    private suspend fun <T> runAuthenticatedRemoteRequest(execute: suspend () -> T): T {
        val runner = authenticatedRequestRunner ?: return execute()

        return when (
            val result =
                runner.run(
                    execute = { execute() },
                    isAuthenticationFailure = ::isAuthenticationFailure,
                )
        ) {
            AuthenticatedRequestResult.MissingSession ->
                throw RouteApiException(
                    httpStatusCode = HTTP_UNAUTHORIZED,
                    status = ROUTE_STATUS_MISSING_SESSION,
                    message = AUTH_REQUIRED_MESSAGE,
                )

            AuthenticatedRequestResult.AuthenticationFailed ->
                throw RouteApiException(
                    httpStatusCode = HTTP_UNAUTHORIZED,
                    status = ROUTE_STATUS_AUTHENTICATION_FAILED,
                    message = AUTH_REQUIRED_MESSAGE,
                )

            is AuthenticatedRequestResult.Success -> result.value
        }
    }

    private fun isAuthenticationFailure(throwable: Throwable): Boolean =
        throwable is RouteApiException &&
            (throwable.httpStatusCode == HTTP_UNAUTHORIZED || throwable.httpStatusCode == HTTP_FORBIDDEN)

    private companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val ROUTE_STATUS_MISSING_SESSION = "ROUTE_AUTH_MISSING_SESSION"
        private const val ROUTE_STATUS_AUTHENTICATION_FAILED = "ROUTE_AUTHENTICATION_FAILED"
        private const val AUTH_REQUIRED_MESSAGE = "인증이 필요합니다."
    }
}

data class RouteSessionData(
    val sessionId: String,
)

data class RouteTransitArrivalData(
    val routeNo: String? = null,
    val remainingMinute: Int? = null,
    val isLowFloor: Boolean? = null,
)

data class RouteTransitRefreshData(
    val type: String,
    val arrivalStatus: String,
    val transits: List<RouteTransitArrivalData> = emptyList(),
)

data class RouteRerouteData(
    val route: RouteCandidate? = null,
)

data class RouteRatingData(
    val ratingId: Long,
)

private fun RouteSessionResponseDto.toRepositoryData(): RouteSessionData =
    RouteSessionData(
        sessionId = sessionId,
    )

private fun RouteTransitRefreshResponseDto.toRepositoryData(): RouteTransitRefreshData =
    RouteTransitRefreshData(
        type = type,
        arrivalStatus = arrivalStatus,
        transits = transits.map(RouteTransitArrivalDto::toRepositoryData),
    )

private fun RouteTransitArrivalDto.toRepositoryData(): RouteTransitArrivalData =
    RouteTransitArrivalData(
        routeNo = routeNo?.trim()?.takeIf(String::isNotEmpty),
        remainingMinute = remainingMinute,
        isLowFloor = isLowFloor,
    )

private fun RouteRerouteResponseDto.toRepositoryData(geometryParser: RouteGeometryParser): RouteRerouteData =
    RouteRerouteData(
        route = toRouteCandidate(geometryParser),
    )

private fun RouteRatingResponseDto.toRepositoryData(): RouteRatingData =
    RouteRatingData(ratingId = ratingId)

private fun GeoCoordinate.toPointDto(): RoutePointDto =
    RoutePointDto(
        lat = latitude,
        lng = longitude,
    )
