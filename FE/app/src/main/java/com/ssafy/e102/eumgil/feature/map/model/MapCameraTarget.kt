package com.ssafy.e102.eumgil.feature.map.model

import com.ssafy.e102.eumgil.core.location.LocationSnapshot

data class MapCoordinate(
    val latitude: Double,
    val longitude: Double,
)

enum class MapCameraSource {
    DEFAULT_BUSAN,
    CURRENT_LOCATION,
}

data class MapCameraTarget(
    val center: MapCoordinate,
    val source: MapCameraSource,
    val requestId: Long = 0L,
) {
    companion object {
        val DefaultBusan =
            MapCameraTarget(
                center = MapDefaults.BUSAN_CENTER,
                source = MapCameraSource.DEFAULT_BUSAN,
            )
    }
}

object MapDefaults {
    val BUSAN_CENTER = MapCoordinate(latitude = 35.1796, longitude = 129.0756)
}

fun LocationSnapshot.toMapCoordinate(): MapCoordinate =
    MapCoordinate(
        latitude = latitude,
        longitude = longitude,
    )
