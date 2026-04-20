package com.ssafy.e102.eumgil.core.model

data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
)

enum class FacilityCategory {
    RESTAURANT,
    TOURIST_ATTRACTION,
    TOILET,
    ELEVATOR,
    CHARGING_STATION,
    BRAILLE_BLOCK,
    OTHER,
}

enum class AccessibilityTag {
    RAMP,
    STEP_FREE_ENTRANCE,
    AUTO_DOOR,
    WIDE_ENTRY,
    ACCESSIBLE_TOILET,
    ELEVATOR,
    WHEELCHAIR_TURNING_SPACE,
    TABLE_SPACING,
    ACCESSIBLE_PARKING,
    LOW_HEIGHT_BUTTON,
    REST_AREA,
    OPEN_24_HOURS,
}

enum class BrailleBlockType {
    GUIDING_LINE,
    WARNING_SURFACE,
    CROSSWALK_APPROACH,
}

data class FacilitySeedQuery(
    val keyword: String? = null,
    val categories: Set<FacilityCategory> = emptySet(),
    val brailleBlockTypes: Set<BrailleBlockType> = emptySet(),
)

data class FacilitySeed(
    val facilityId: String,
    val name: String,
    val address: String,
    val coordinate: GeoCoordinate,
    val category: FacilityCategory,
    val accessibilityTags: List<AccessibilityTag> = emptyList(),
    val brailleBlockType: BrailleBlockType? = null,
    val description: String? = null,
) {
    init {
        require(category == FacilityCategory.BRAILLE_BLOCK || brailleBlockType == null) {
            "Braille block type can only be assigned to BRAILLE_BLOCK seeds."
        }
    }
}

data class FacilityMarkerSeed(
    val facilityId: String,
    val name: String,
    val coordinate: GeoCoordinate,
    val category: FacilityCategory,
    val accessibilityTags: List<AccessibilityTag> = emptyList(),
    val brailleBlockType: BrailleBlockType? = null,
)

data class FacilityDetailSeed(
    val facilityId: String,
    val name: String,
    val address: String,
    val coordinate: GeoCoordinate,
    val category: FacilityCategory,
    val accessibilityTags: List<AccessibilityTag> = emptyList(),
    val brailleBlockType: BrailleBlockType? = null,
    val description: String? = null,
)
