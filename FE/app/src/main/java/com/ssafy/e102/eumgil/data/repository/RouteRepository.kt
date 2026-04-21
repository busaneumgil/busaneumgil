package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSearchResult
import com.ssafy.e102.eumgil.core.model.RouteSearchSource
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.RouteMockDataSource
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
    private val mockDataSource: RouteMockDataSource,
    private val geometryParser: RouteGeometryParser = DefaultRouteGeometryParser(),
) : RouteRepository {
    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData {
        localDataSource.getCachedSearchData(query)?.let { cachedSearchData ->
            return cachedSearchData.copy(source = cachedSearchData.source.asCached())
        }

        val fixturePayload = mockDataSource.searchRouteFixture(query.toRequestDto())
        val result = fixturePayload.response.toDomain(query = query, geometryParser = geometryParser)
        val searchData =
            RouteSearchData(
                query = query,
                result = result,
                source =
                    RouteSearchSource.mockFixture(
                        fixtureId = fixturePayload.fixtureId,
                        label = fixturePayload.fixtureName,
                    ),
            )
        localDataSource.updateCachedSearchData(query = query, searchData = searchData)
        return searchData
    }
}
