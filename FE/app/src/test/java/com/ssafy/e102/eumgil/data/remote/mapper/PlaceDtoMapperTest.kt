package com.ssafy.e102.eumgil.data.remote.mapper

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceFeatureType
import com.ssafy.e102.eumgil.data.remote.dto.PlaceAccessibilityFeatureDto
import com.ssafy.e102.eumgil.data.remote.dto.PlaceDetailDto
import com.ssafy.e102.eumgil.data.remote.dto.PlacePointDto
import com.ssafy.e102.eumgil.data.remote.dto.PlaceSummaryDto
import com.ssafy.e102.eumgil.data.remote.dto.PlacesBrowseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceDtoMapperTest {
    @Test
    fun `toPlaceSummaries keeps the API category and all available accessibility tag keys`() {
        val summaries =
            PlaceDtoMapper.toPlaceSummaries(
                PlacesBrowseDto(
                    places =
                        listOf(
                            PlaceSummaryDto(
                                placeId = 88L,
                                name = "Remote Welfare Center",
                                category = "WELFARE",
                                address = "88 Welfare-ro, Busan",
                                point = PlacePointDto(lat = 35.1801, lng = 129.0722),
                                accessibilityFeatures =
                                    listOf(
                                        PlaceAccessibilityFeatureDto(
                                            featureType = "guidanceFacility",
                                            isAvailable = true,
                                        ),
                                        PlaceAccessibilityFeatureDto(
                                            featureType = "accessibleRoom",
                                            isAvailable = true,
                                        ),
                                        PlaceAccessibilityFeatureDto(
                                            featureType = "accessibleToilet",
                                            isAvailable = true,
                                        ),
                                        PlaceAccessibilityFeatureDto(
                                            featureType = "accessibleParking",
                                            isAvailable = false,
                                        ),
                                    ),
                                isBookmarked = false,
                            ),
                        ),
                ),
            )

        assertEquals(1, summaries.size)
        assertEquals(PlaceCategory.WELFARE, summaries.first().category)
        assertEquals(
            listOf(
                PlaceFeatureType.GUIDANCE_FACILITY,
                PlaceFeatureType.ACCESSIBLE_ROOM,
                PlaceFeatureType.ACCESSIBLE_TOILET,
                PlaceFeatureType.ACCESSIBLE_PARKING,
            ),
            summaries.first().features.map { feature -> feature.featureType },
        )
        assertEquals(
            listOf("guidance-facility", "accessible-room", "accessible-toilet"),
            summaries.first().accessibilityTags,
        )
    }

    @Test
    fun `toPlaceDetail keeps description when present and normalizes blank optional values`() {
        val detail =
            PlaceDtoMapper.toPlaceDetail(
                PlaceDetailDto(
                    placeId = 101L,
                    name = "Barrier-Free Hotel",
                    category = "ACCOMMODATION",
                    address = "101 Ocean-ro, Busan",
                    point = PlacePointDto(lat = 35.166, lng = 129.072),
                    providerPlaceId = "   ",
                    accessibilityFeatures =
                        listOf(
                            PlaceAccessibilityFeatureDto(
                                featureType = "elevator",
                                isAvailable = true,
                            ),
                        ),
                    isBookmarked = true,
                    description = "Wheelchair-friendly lobby and rooms",
                ),
            )

        assertEquals(PlaceCategory.ACCOMMODATION, detail.category)
        assertEquals(listOf(PlaceFeatureType.ELEVATOR), detail.features.map { feature -> feature.featureType })
        assertEquals(listOf("elevator"), detail.accessibilityTags)
        assertEquals("Wheelchair-friendly lobby and rooms", detail.description)
        assertNull(detail.providerPlaceId)
    }
}
