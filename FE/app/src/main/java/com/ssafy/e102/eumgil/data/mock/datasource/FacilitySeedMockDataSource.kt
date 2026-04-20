package com.ssafy.e102.eumgil.data.mock.datasource

import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.FacilityMarkerSeed
import com.ssafy.e102.eumgil.core.model.FacilitySeedQuery
import com.ssafy.e102.eumgil.data.mock.fixture.MockFacilitySeedFixtures

class FacilitySeedMockDataSource {
    suspend fun getFacilityMarkers(query: FacilitySeedQuery): List<FacilityMarkerSeed> =
        MockFacilitySeedFixtures.getFacilityMarkers(query)

    suspend fun getFacilityDetail(facilityId: String): FacilityDetailSeed? =
        MockFacilitySeedFixtures.getFacilityDetail(facilityId)
}
