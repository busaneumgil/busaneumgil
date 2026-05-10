package com.ssafy.e102.eumgil.data.repository

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
import com.ssafy.e102.eumgil.data.route.toDomain
import com.ssafy.e102.eumgil.data.route.toRequestDto

interface RouteRepository {
    // Primary read-model entry point for 199 route setting and 200/201/202 handoff consumers.
    suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData

    suspend fun searchRoutes(query: RouteSearchQuery): RouteSearchResult = getRouteSearchData(query).result
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

    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData {
        localDataSource.getCachedSearchData(query)?.let { cachedSearchData ->
            return cachedSearchData.copy(source = cachedSearchData.source.asCached())
        }

        val response = runAuthenticatedRemoteRequest { remoteDataSource.searchWalkRoutes(query.toRequestDto()) }
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
