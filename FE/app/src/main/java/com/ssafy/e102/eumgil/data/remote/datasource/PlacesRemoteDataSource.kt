package com.ssafy.e102.eumgil.data.remote.datasource

import com.ssafy.e102.eumgil.core.model.PlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceQuery
import com.ssafy.e102.eumgil.core.model.PlaceSummary

class PlacesRemoteDataSource(
    private val baseUrl: String,
) {
    suspend fun getPlaces(query: PlaceQuery): List<PlaceSummary> {
        error("TODO: connect live places API using $baseUrl for query=$query")
    }

    suspend fun getPlaceDetail(placeId: String): PlaceDetail? {
        error("TODO: connect live place detail API using $baseUrl for placeId=$placeId")
    }
}
