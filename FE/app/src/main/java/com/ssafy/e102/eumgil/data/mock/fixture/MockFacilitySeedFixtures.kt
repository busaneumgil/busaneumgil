package com.ssafy.e102.eumgil.data.mock.fixture

import com.ssafy.e102.eumgil.core.model.AccessibilityTag
import com.ssafy.e102.eumgil.core.model.BrailleBlockType
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.FacilityMarkerSeed
import com.ssafy.e102.eumgil.core.model.FacilitySeed
import com.ssafy.e102.eumgil.core.model.FacilitySeedQuery
import com.ssafy.e102.eumgil.core.model.GeoCoordinate

object MockFacilitySeedFixtures {
    private val facilitySeeds =
        listOf(
            FacilitySeed(
                facilityId = "facility-toilet-1",
                name = "Haeundae Station Accessible Toilet",
                address = "35 Jungdong 1-ro, Haeundae-gu, Busan",
                coordinate = GeoCoordinate(latitude = 35.16305, longitude = 129.15872),
                category = FacilityCategory.TOILET,
                accessibilityTags =
                    listOf(
                        AccessibilityTag.WHEELCHAIR_TURNING_SPACE,
                        AccessibilityTag.AUTO_DOOR,
                        AccessibilityTag.ELEVATOR,
                    ),
                description = "Accessible restroom near Haeundae Station Exit 1.",
            ),
            FacilitySeed(
                facilityId = "facility-restaurant-1",
                name = "Cafe Ondo",
                address = "123 Haeundae-ro, Haeundae-gu, Busan",
                coordinate = GeoCoordinate(latitude = 35.16241, longitude = 129.15994),
                category = FacilityCategory.RESTAURANT,
                accessibilityTags =
                    listOf(
                        AccessibilityTag.RAMP,
                        AccessibilityTag.AUTO_DOOR,
                        AccessibilityTag.TABLE_SPACING,
                        AccessibilityTag.ACCESSIBLE_PARKING,
                    ),
                description = "Cafe mock seed with ramp access and generous table spacing.",
            ),
            FacilitySeed(
                facilityId = "facility-restaurant-2",
                name = "Haebyeonmaru",
                address = "42 Gunam-ro, Haeundae-gu, Busan",
                coordinate = GeoCoordinate(latitude = 35.16094, longitude = 129.15796),
                category = FacilityCategory.RESTAURANT,
                accessibilityTags =
                    listOf(
                        AccessibilityTag.STEP_FREE_ENTRANCE,
                        AccessibilityTag.ACCESSIBLE_TOILET,
                        AccessibilityTag.TABLE_SPACING,
                        AccessibilityTag.ELEVATOR,
                    ),
                description = "Restaurant seed used for marker overlap and filter checks.",
            ),
            FacilitySeed(
                facilityId = "facility-toilet-2",
                name = "Central Transit Plaza Accessible Toilet",
                address = "539-10 U-dong, Haeundae-gu, Busan",
                coordinate = GeoCoordinate(latitude = 35.16047, longitude = 129.16038),
                category = FacilityCategory.TOILET,
                accessibilityTags =
                    listOf(
                        AccessibilityTag.WIDE_ENTRY,
                        AccessibilityTag.AUTO_DOOR,
                        AccessibilityTag.OPEN_24_HOURS,
                    ),
                description = "Transit plaza restroom kept in the seed set for detail sheet validation.",
            ),
            FacilitySeed(
                facilityId = "facility-elevator-1",
                name = "Haeundae Station Exit 1 Elevator",
                address = "35 Jungdong 1-ro, Haeundae-gu, Busan",
                coordinate = GeoCoordinate(latitude = 35.16312, longitude = 129.16053),
                category = FacilityCategory.ELEVATOR,
                accessibilityTags =
                    listOf(
                        AccessibilityTag.WIDE_ENTRY,
                        AccessibilityTag.LOW_HEIGHT_BUTTON,
                    ),
                description = "Station to street elevator connection used as a standalone facility seed.",
            ),
            FacilitySeed(
                facilityId = "facility-charging-1",
                name = "U-dong Public Parking Wheelchair Charger",
                address = "541-3 U-dong, Haeundae-gu, Busan",
                coordinate = GeoCoordinate(latitude = 35.16035, longitude = 129.16061),
                category = FacilityCategory.CHARGING_STATION,
                accessibilityTags =
                    listOf(
                        AccessibilityTag.STEP_FREE_ENTRANCE,
                        AccessibilityTag.OPEN_24_HOURS,
                    ),
                description = "Wheelchair charging station mock seed for map marker rendering.",
            ),
            FacilitySeed(
                facilityId = "facility-braille-1",
                name = "Haeundae Station Exit 1 Braille Guide",
                address = "35 Jungdong 1-ro, Haeundae-gu, Busan",
                coordinate = GeoCoordinate(latitude = 35.16318, longitude = 129.15948),
                category = FacilityCategory.BRAILLE_BLOCK,
                brailleBlockType = BrailleBlockType.GUIDING_LINE,
                accessibilityTags = listOf(AccessibilityTag.STEP_FREE_ENTRANCE),
                description = "Guide line segment connecting station exit and pedestrian path.",
            ),
            FacilitySeed(
                facilityId = "facility-braille-2",
                name = "Gunam-ro Crosswalk Braille Block",
                address = "58 Gunam-ro, Haeundae-gu, Busan",
                coordinate = GeoCoordinate(latitude = 35.16184, longitude = 129.15922),
                category = FacilityCategory.BRAILLE_BLOCK,
                brailleBlockType = BrailleBlockType.CROSSWALK_APPROACH,
                accessibilityTags = listOf(AccessibilityTag.REST_AREA),
                description = "Crosswalk approach block kept separate for future overlay styling.",
            ),
        )

    fun getFacilityMarkers(query: FacilitySeedQuery = FacilitySeedQuery()): List<FacilityMarkerSeed> {
        val normalizedKeyword = query.keyword?.trim()?.lowercase().orEmpty()

        return facilitySeeds
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
        facilitySeeds.firstOrNull { seed -> seed.facilityId == facilityId }?.toDetailSeed()

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
