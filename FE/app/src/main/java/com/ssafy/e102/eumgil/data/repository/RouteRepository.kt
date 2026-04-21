package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSearchResult
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.RouteMockDataSource
import com.ssafy.e102.eumgil.data.route.DefaultRouteGeometryParser
import com.ssafy.e102.eumgil.data.route.RouteGeometryParser
import com.ssafy.e102.eumgil.data.route.toDomain
import com.ssafy.e102.eumgil.data.route.toRequestDto

interface RouteRepository {
    suspend fun searchRoutes(query: RouteSearchQuery): RouteSearchResult
}

class DefaultRouteRepository(
    private val localDataSource: RouteLocalDataSource,
    private val mockDataSource: RouteMockDataSource,
    private val geometryParser: RouteGeometryParser = DefaultRouteGeometryParser(),
) : RouteRepository {
    override suspend fun searchRoutes(query: RouteSearchQuery): RouteSearchResult {
        localDataSource.getCachedSearchResult(query)?.let { cachedResult ->
            return cachedResult
        }

        val response = mockDataSource.searchRoutes(query.toRequestDto())
        val result = response.toDomain(query = query, geometryParser = geometryParser)
        localDataSource.updateCachedSearchResult(query = query, result = result)
        return result
    }
}
