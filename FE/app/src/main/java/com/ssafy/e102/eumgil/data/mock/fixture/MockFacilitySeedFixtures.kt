package com.ssafy.e102.eumgil.data.mock.fixture

import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.FacilityMarkerSeed
import com.ssafy.e102.eumgil.core.model.FacilitySeed
import com.ssafy.e102.eumgil.core.model.FacilitySeedCatalog
import com.ssafy.e102.eumgil.core.model.FacilitySeedQuery
import com.ssafy.e102.eumgil.data.mock.seed.MockFacilitySeedCatalog

object MockFacilitySeedFixtures {
    fun getSeedCatalog(): FacilitySeedCatalog = MockFacilitySeedCatalog.catalog

    fun getFacilityMarkers(query: FacilitySeedQuery = FacilitySeedQuery()): List<FacilityMarkerSeed> {
        val normalizedKeyword = query.keyword?.trim()?.lowercase().orEmpty()

        return getSeedCatalog()
            .allSeeds
            .asSequence()
            .filter { seed ->
                normalizedKeyword.isBlank() ||
                    seed.name.lowercase().contains(normalizedKeyword) ||
                    seed.address.lowercase().contains(normalizedKeyword)
            }.filter { seed ->
                query.categories.isEmpty() || seed.category in query.categories
            }.filter { seed ->
                query.brailleBlockTypes.isEmpty() || seed.brailleBlockType in query.brailleBlockTypes
            }.map { seed -> seed.toMarkerSeed() }
            .toList()
    }

    fun getFacilityDetail(facilityId: String): FacilityDetailSeed? =
        getSeedCatalog().allSeeds.firstOrNull { seed -> seed.facilityId == facilityId }?.toDetailSeed()

    private fun FacilitySeed.toMarkerSeed(): FacilityMarkerSeed =
        FacilityMarkerSeed(
            facilityId = facilityId,
            name = name,
            coordinate = coordinate,
            category = category,
            accessibilityTags = accessibilityTags,
            brailleBlockType = brailleBlockType,
        )

    private fun FacilitySeed.toDetailSeed(): FacilityDetailSeed =
        FacilityDetailSeed(
            facilityId = facilityId,
            name = name,
            address = address,
            coordinate = coordinate,
            category = category,
            accessibilityTags = accessibilityTags,
            brailleBlockType = brailleBlockType,
            description = description,
        )
}
