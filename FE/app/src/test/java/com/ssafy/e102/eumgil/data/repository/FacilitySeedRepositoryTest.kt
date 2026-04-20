package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.BrailleBlockType
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilitySeedQuery
import com.ssafy.e102.eumgil.data.mock.datasource.FacilitySeedMockDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FacilitySeedRepositoryTest {
    private val repository: FacilitySeedRepository =
        DefaultFacilitySeedRepository(
            mockDataSource = FacilitySeedMockDataSource(),
        )

    @Test
    fun `getFacilityMarkers returns both facility and braille block seeds`() =
        runBlocking {
            val markers = repository.getFacilityMarkers()

            assertEquals(8, markers.size)
            assertEquals("facility-toilet-1", markers.first().facilityId)
            assertEquals("facility-braille-2", markers.last().facilityId)
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
            assertEquals("facility-braille-2", markers.single().facilityId)
        }

    @Test
    fun `getFacilityDetail returns detail seed for bottom sheet consumers`() =
        runBlocking {
            val detail = repository.getFacilityDetail("facility-elevator-1")

            assertNotNull(detail)
            assertEquals(FacilityCategory.ELEVATOR, detail?.category)
        }
}
