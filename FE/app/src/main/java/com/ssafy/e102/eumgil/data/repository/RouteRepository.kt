package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSearchResult
import com.ssafy.e102.eumgil.core.model.RouteSearchSource
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
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
) : RouteRepository {
    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData {
        localDataSource.getCachedSearchData(query)?.let { cachedSearchData ->
            return cachedSearchData.copy(source = cachedSearchData.source.asCached())
        }

        val response = remoteDataSource.searchWalkRoutes(query.toRequestDto())
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
}
