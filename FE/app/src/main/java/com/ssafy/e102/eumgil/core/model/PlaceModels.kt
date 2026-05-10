package com.ssafy.e102.eumgil.core.model

enum class PlaceCategory {
    TOILET,
    ELEVATOR,
    CHARGING_STATION,
    FOOD_CAFE,
    TOURIST_SPOT,
    ACCOMMODATION,
    HEALTHCARE,
    WELFARE,
    PUBLIC_OFFICE,
    BRAILLE_BLOCK,
    RESTAURANT,
    TOURIST_ATTRACTION,
    OTHER,
}

enum class PlaceFeatureType {
    ACCESSIBLE_ENTRANCE,
    ELEVATOR,
    ACCESSIBLE_TOILET,
    ACCESSIBLE_PARKING,
    CHARGING_STATION,
    ACCESSIBLE_ROOM,
    GUIDANCE_FACILITY,
}

data class PlaceFeatureAvailability(
    val featureType: PlaceFeatureType,
    val isAvailable: Boolean,
)

data class PlaceQuery(
    val keyword: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = DEFAULT_PLACE_BROWSE_RADIUS_METERS,
    val categories: Set<PlaceCategory> = emptySet(),
    val featureTypes: Set<PlaceFeatureType> = emptySet(),
)

data class PlaceSummary(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory,
    val features: List<PlaceFeatureAvailability> = emptyList(),
    val isBookmarked: Boolean = false,
    val accessibilityTags: List<String> = emptyList(),
)

data class PlaceDetail(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory,
    val features: List<PlaceFeatureAvailability> = emptyList(),
    val isBookmarked: Boolean = false,
    val accessibilityTags: List<String> = emptyList(),
    val providerPlaceId: String? = null,
    val description: String? = null,
)

private const val DEFAULT_PLACE_BROWSE_RADIUS_METERS = 1_000
