package com.ssafy.e102.eumgil.feature.lowvision

import androidx.annotation.DrawableRes
import com.ssafy.e102.eumgil.R
import java.util.Locale

internal object LowVisionPlaceCardDefaults {
    @DrawableRes
    val saveIconRes: Int = R.drawable.ic_action_favorite

    @DrawableRes
    val routeIconRes: Int = R.drawable.ic_nav_route
}

internal fun lowVisionBriefAddress(address: String?): String {
    val normalized = address?.trim().orEmpty()
    if (normalized.isEmpty()) return "GPS 기반 위치"

    val segments = normalized.split(Regex("\\s+"))
    return if (segments.size <= 3) {
        normalized
    } else {
        segments.takeLast(3).joinToString(separator = " ")
    }
}

internal fun lowVisionDetailAddress(
    address: String?,
    latitude: Double,
    longitude: Double,
): String =
    "상세 주소: ${address?.trim()?.takeIf { it.isNotEmpty() } ?: "GPS 기반 위치"}\n" +
        "GPS 위치: 위도 ${latitude.toLowVisionCoordinateText()}, 경도 ${longitude.toLowVisionCoordinateText()}"

private fun Double.toLowVisionCoordinateText(): String =
    String.format(Locale.US, "%.5f", this)
