package com.ssafy.e102.eumgil.data.local.datasource

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceQuery
import com.ssafy.e102.eumgil.core.model.PlaceSummary
import java.util.concurrent.ConcurrentHashMap

class PlacesLocalDataSource {
    private val placeCacheByQuery = ConcurrentHashMap<String, List<PlaceSummary>>()
    private val placeDetailCacheById = ConcurrentHashMap<String, PlaceDetail>()

    suspend fun getCachedPlaces(query: PlaceQuery): List<PlaceSummary> =
        placeCacheByQuery[query.cacheKey()].orEmpty()

    suspend fun updateCachedPlaces(
        query: PlaceQuery,
        places: List<PlaceSummary>,
    ) {
        placeCacheByQuery[query.cacheKey()] = places
    }

    suspend fun getCachedPlaceDetail(placeId: String): PlaceDetail? = placeDetailCacheById[placeId]

    suspend fun updateCachedPlaceDetail(placeDetail: PlaceDetail) {
        placeDetailCacheById[placeDetail.placeId] = placeDetail
    }

    private fun PlaceQuery.cacheKey(): String =
        listOf(
            keyword?.trim().orEmpty(),
            latitude?.toString().orEmpty(),
            longitude?.toString().orEmpty(),
            categories
                .sortedBy(PlaceCategory::name)
                .joinToString(separator = ","),
        ).joinToString(separator = "|")
}
