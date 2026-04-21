package com.ssafy.e102.eumgil.core.model

data class PlaceDestination(
    val placeId: String,
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
)

// Search, facility detail, and saved-place handoff all converge on the same minimal destination payload.
fun SearchResult.toPlaceDestination(): PlaceDestination =
    PlaceDestination(
        placeId = placeId,
        name = title,
        address = subtitle.takeIf { it.isNotBlank() },
        latitude = latitude,
        longitude = longitude,
    )

// 119 fixes the facility-detail handoff contract here so 200/214 can reuse it without branching by source.
fun FacilityDetailSeed.toPlaceDestination(): PlaceDestination =
    PlaceDestination(
        placeId = facilityId,
        name = name,
        address = address.takeIf { it.isNotBlank() },
        latitude = coordinate.latitude,
        longitude = coordinate.longitude,
    )
