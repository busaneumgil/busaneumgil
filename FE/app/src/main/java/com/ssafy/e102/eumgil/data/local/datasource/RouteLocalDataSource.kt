package com.ssafy.e102.eumgil.data.local.datasource

import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class RouteLocalDataSource {
    private val searchDataByQuery = ConcurrentHashMap<String, RouteSearchData>()

    suspend fun getCachedSearchData(query: RouteSearchQuery): RouteSearchData? =
        searchDataByQuery[query.cacheKey()]

    suspend fun updateCachedSearchData(
        query: RouteSearchQuery,
        searchData: RouteSearchData,
    ) {
        searchDataByQuery[query.cacheKey()] = searchData
    }

    private fun RouteSearchQuery.cacheKey(): String =
        listOf(
            origin.cacheKey(),
            destination.cacheKey(),
            requestedOptions.joinToString(separator = ",") { routeOption -> routeOption.name },
        ).joinToString(separator = "|")

    private fun RouteWaypoint.cacheKey(): String =
        listOf(
            placeId.normalized(),
            name.normalized(),
            address.normalized(),
            coordinate.latitude.toCacheCoordinate(),
            coordinate.longitude.toCacheCoordinate(),
        ).joinToString(separator = "~")

    private fun String?.normalized(): String = this?.trim()?.lowercase().orEmpty()

    private fun Double.toCacheCoordinate(): String = String.format(Locale.US, "%.6f", this)
}
