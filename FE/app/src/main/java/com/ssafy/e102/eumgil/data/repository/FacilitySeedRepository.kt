package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.FacilityMarkerSeed
import com.ssafy.e102.eumgil.core.model.FacilitySeedQuery
import com.ssafy.e102.eumgil.data.mock.datasource.FacilitySeedMockDataSource

interface FacilitySeedRepository {
    suspend fun getFacilityMarkers(query: FacilitySeedQuery = FacilitySeedQuery()): List<FacilityMarkerSeed>

    suspend fun getFacilityDetail(facilityId: String): FacilityDetailSeed?
}

class DefaultFacilitySeedRepository(
    private val mockDataSource: FacilitySeedMockDataSource,
) : FacilitySeedRepository {
    override suspend fun getFacilityMarkers(query: FacilitySeedQuery): List<FacilityMarkerSeed> =
        mockDataSource.getFacilityMarkers(query)

    override suspend fun getFacilityDetail(facilityId: String): FacilityDetailSeed? =
        mockDataSource.getFacilityDetail(facilityId)
}
