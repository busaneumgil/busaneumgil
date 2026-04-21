package com.ssafy.e102.eumgil.data.route

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RoutePolyline

interface RouteGeometryParser {
    fun parse(geometry: String?): RouteGeometryParseResult
}

data class RouteGeometryParseResult(
    val polyline: RoutePolyline = RoutePolyline(),
    val status: RouteGeometryParseStatus,
)

enum class RouteGeometryParseStatus {
    SUCCESS,
    EMPTY_INPUT,
    UNSUPPORTED_GEOMETRY,
    INVALID_COORDINATE,
    INSUFFICIENT_POINTS,
}

class DefaultRouteGeometryParser : RouteGeometryParser {
    override fun parse(geometry: String?): RouteGeometryParseResult {
        val normalizedGeometry = geometry?.trim().orEmpty()
        if (normalizedGeometry.isBlank()) {
            return RouteGeometryParseResult(status = RouteGeometryParseStatus.EMPTY_INPUT)
        }

        val geometryType = normalizedGeometry.substringBefore("(", missingDelimiterValue = "").trim()
        if (!geometryType.equals(LINESTRING_TYPE, ignoreCase = true)) {
            return RouteGeometryParseResult(status = RouteGeometryParseStatus.UNSUPPORTED_GEOMETRY)
        }

        val coordinatePayload =
            normalizedGeometry
                .substringAfter("(", missingDelimiterValue = "")
                .substringBeforeLast(")", missingDelimiterValue = "")
                .trim()

        if (coordinatePayload.isBlank()) {
            return RouteGeometryParseResult(status = RouteGeometryParseStatus.INVALID_COORDINATE)
        }

        val rawCoordinates = coordinatePayload.split(",")
        if (rawCoordinates.size < MINIMUM_RENDERABLE_POINT_COUNT) {
            return RouteGeometryParseResult(status = RouteGeometryParseStatus.INSUFFICIENT_POINTS)
        }

        val coordinates =
            rawCoordinates.map { rawCoordinate ->
                parseCoordinate(rawCoordinate)
                    ?: return RouteGeometryParseResult(status = RouteGeometryParseStatus.INVALID_COORDINATE)
            }

        return RouteGeometryParseResult(
            polyline = RoutePolyline(points = coordinates),
            status = RouteGeometryParseStatus.SUCCESS,
        )
    }

    private fun parseCoordinate(rawCoordinate: String): GeoCoordinate? {
        val parts = rawCoordinate.trim().split(COORDINATE_SEPARATOR)
        if (parts.size < 2) return null

        val longitude = parts[0].toDoubleOrNull() ?: return null
        val latitude = parts[1].toDoubleOrNull() ?: return null

        return GeoCoordinate(
            latitude = latitude,
            longitude = longitude,
        )
    }

    companion object {
        private const val LINESTRING_TYPE: String = "LINESTRING"
        private const val MINIMUM_RENDERABLE_POINT_COUNT: Int = 2
        private val COORDINATE_SEPARATOR = Regex("\\s+")
    }
}
