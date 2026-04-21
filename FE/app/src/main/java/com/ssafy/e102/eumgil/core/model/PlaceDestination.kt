package com.ssafy.e102.eumgil.core.model

data class PlaceDestination(
    val placeId: String,
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
)

fun SearchResult.toPlaceDestination(): PlaceDestination =
    PlaceDestination(
        placeId = placeId,
        name = title,
        address = subtitle.takeIf { it.isNotBlank() },
        latitude = latitude,
        longitude = longitude,
    )

fun FacilityDetailSeed.toPlaceDestination(): PlaceDestination =
    PlaceDestination(
        placeId = facilityId,
        name = name,
        address = address.takeIf { it.isNotBlank() },
        latitude = coordinate.latitude,
        longitude = coordinate.longitude,
    )
