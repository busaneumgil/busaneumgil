package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import java.util.Locale

data class LowVisionCurrentLocationDisplay(
    val title: String,
    val supportingText: String,
    val talkBackText: String,
)

internal fun lowVisionCurrentLocationDisplay(snapshot: LocationSnapshot?): LowVisionCurrentLocationDisplay =
    lowVisionCurrentLocationDisplay(
        snapshot = snapshot,
        address = null,
    )

internal fun lowVisionCurrentLocationDisplay(
    snapshot: LocationSnapshot?,
    address: String?,
): LowVisionCurrentLocationDisplay =
    lowVisionCurrentLocationDisplay(
        latitude = snapshot?.latitude,
        longitude = snapshot?.longitude,
        address = address,
    )

internal fun lowVisionCurrentLocationDisplay(coordinate: GeoCoordinate?): LowVisionCurrentLocationDisplay =
    lowVisionCurrentLocationDisplay(
        coordinate = coordinate,
        address = null,
    )

internal fun lowVisionCurrentLocationDisplay(
    coordinate: GeoCoordinate?,
    address: String?,
): LowVisionCurrentLocationDisplay =
    lowVisionCurrentLocationDisplay(
        latitude = coordinate?.latitude,
        longitude = coordinate?.longitude,
        address = address,
    )

internal fun lowVisionCurrentLocationDisplay(
    latitude: Double?,
    longitude: Double?,
    address: String? = null,
): LowVisionCurrentLocationDisplay {
    val addressText = address?.trim().orEmpty()
    if (addressText.isNotEmpty()) {
        return LowVisionCurrentLocationDisplay(
            title = LOW_VISION_CURRENT_LOCATION_TITLE,
            supportingText = "",
            talkBackText = "$LOW_VISION_CURRENT_LOCATION_TITLE $addressText",
        )
    }

    if (latitude == null || longitude == null) {
        return LowVisionCurrentLocationDisplay(
            title = LOW_VISION_CURRENT_LOCATION_TITLE,
            supportingText = "",
            talkBackText = LOW_VISION_CURRENT_LOCATION_TITLE,
        )
    }

    val latitudeText = latitude.toLowVisionGpsText()
    val longitudeText = longitude.toLowVisionGpsText()
    return LowVisionCurrentLocationDisplay(
        title = LOW_VISION_CURRENT_LOCATION_TITLE,
        supportingText = "",
        talkBackText = "$LOW_VISION_CURRENT_LOCATION_TITLE 위도 $latitudeText\uB3C4 경도 $longitudeText\uB3C4",
    )
}

private fun Double.toLowVisionGpsText(): String = String.format(Locale.US, "%.5f", this)

private const val LOW_VISION_CURRENT_LOCATION_TITLE = "현재 위치"
