package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.BrailleBlockType
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilitySeedQuery
import com.ssafy.e102.eumgil.data.mock.datasource.FacilitySeedMockDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FacilitySeedRepositoryTest {
    private val repository: FacilitySeedRepository =
        DefaultFacilitySeedRepository(
            mockDataSource = FacilitySeedMockDataSource(),
        )

    @Test
    fun `getSeedCatalog separates facilities and braille blocks for later repository reads`() =
        runBlocking {
            val catalog = repository.getSeedCatalog()

            assertEquals(9, catalog.facilities.size)
            assertEquals(4, catalog.brailleBlocks.size)
            assertEquals(13, catalog.allSeeds.size)
            assertTrue(catalog.facilities.all { seed -> seed.category != FacilityCategory.BRAILLE_BLOCK })
            assertTrue(catalog.brailleBlocks.all { seed -> seed.category == FacilityCategory.BRAILLE_BLOCK })
        }

    @Test
    fun `getSeedCatalog covers marker categories and braille block types`() =
        runBlocking {
            val catalog = repository.getSeedCatalog()

            assertEquals(
                setOf(
                    FacilityCategory.RESTAURANT,
                    FacilityCategory.TOURIST_ATTRACTION,
                    FacilityCategory.TOILET,
                    FacilityCategory.ELEVATOR,
                    FacilityCategory.CHARGING_STATION,
                ),
                catalog.facilities.map { seed -> seed.category }.toSet(),
            )
            assertEquals(
                setOf(
                    BrailleBlockType.GUIDING_LINE,
                    BrailleBlockType.WARNING_SURFACE,
                    BrailleBlockType.CROSSWALK_APPROACH,
                ),
                catalog.brailleBlocks.mapNotNull { seed -> seed.brailleBlockType }.toSet(),
            )
        }

    @Test
    fun `getFacilityMarkers filters by category and braille block type`() =
        runBlocking {
            val markers =
                repository.getFacilityMarkers(
                    FacilitySeedQuery(
                        categories = setOf(FacilityCategory.BRAILLE_BLOCK),
                        brailleBlockTypes = setOf(BrailleBlockType.CROSSWALK_APPROACH),
                    ),
                )

            assertEquals(1, markers.size)
            assertEquals("braille-crosswalk-gunamro-1", markers.single().facilityId)
        }

    @Test
    fun `getFacilityDetail returns detail seed for bottom sheet consumers`() =
        runBlocking {
            val detail = repository.getFacilityDetail("facility-elevator-haeundae-exit1-1")

            assertNotNull(detail)
            assertEquals(FacilityCategory.ELEVATOR, detail?.category)
        }
}
