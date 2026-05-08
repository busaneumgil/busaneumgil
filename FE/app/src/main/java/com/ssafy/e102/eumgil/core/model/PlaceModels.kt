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

data class PlaceQuery(
    val keyword: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val categories: Set<PlaceCategory> = emptySet(),
)

data class PlaceSummary(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory,
    val accessibilityTags: List<String> = emptyList(),
)

data class PlaceDetail(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory,
    val accessibilityTags: List<String> = emptyList(),
    val description: String? = null,
)
