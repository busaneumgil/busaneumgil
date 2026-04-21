package com.ssafy.e102.eumgil.data.route

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RoutePolyline

interface RouteGeometryParser {
    fun parse(geometry: String?): RouteGeometryParseResult
}

data class RouteGeometryParseResult(
    val polyline: RoutePolyline = RoutePolyline(),
    val status: RouteGeometryParseStatus,
    val parsedPointCount: Int = 0,
)

enum class RouteGeometryParseStatus {
    SUCCESS,
    EMPTY_INPUT,
    MALFORMED_GEOMETRY,
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

        val openingParenthesisIndex = normalizedGeometry.indexOf('(')
        val closingParenthesisIndex = normalizedGeometry.lastIndexOf(')')
        if (
            openingParenthesisIndex <= 0 ||
            closingParenthesisIndex <= openingParenthesisIndex ||
            closingParenthesisIndex != normalizedGeometry.lastIndex
        ) {
            return RouteGeometryParseResult(status = RouteGeometryParseStatus.MALFORMED_GEOMETRY)
        }

        val geometryType = normalizedGeometry.substring(0, openingParenthesisIndex).trim()
        if (geometryType.isBlank()) {
            return RouteGeometryParseResult(status = RouteGeometryParseStatus.MALFORMED_GEOMETRY)
        }
        if (!geometryType.equals(LINESTRING_TYPE, ignoreCase = true)) {
            return RouteGeometryParseResult(status = RouteGeometryParseStatus.UNSUPPORTED_GEOMETRY)
        }

        val coordinatePayload =
            normalizedGeometry
                .substring(openingParenthesisIndex + 1, closingParenthesisIndex)
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
            parsedPointCount = coordinates.size,
        )
    }

    private fun parseCoordinate(rawCoordinate: String): GeoCoordinate? {
        val parts = rawCoordinate.trim().split(COORDINATE_SEPARATOR)
        if (parts.size < 2) return null

        val longitude = parts[0].toDoubleOrNull() ?: return null
        val latitude = parts[1].toDoubleOrNull() ?: return null
        if (longitude !in MIN_LONGITUDE..MAX_LONGITUDE || latitude !in MIN_LATITUDE..MAX_LATITUDE) {
            return null
        }

        return GeoCoordinate(
            latitude = latitude,
            longitude = longitude,
        )
    }

    companion object {
        private const val LINESTRING_TYPE: String = "LINESTRING"
        private const val MINIMUM_RENDERABLE_POINT_COUNT: Int = 2
        private const val MIN_LATITUDE: Double = -90.0
        private const val MAX_LATITUDE: Double = 90.0
        private const val MIN_LONGITUDE: Double = -180.0
        private const val MAX_LONGITUDE: Double = 180.0
        private val COORDINATE_SEPARATOR = Regex("\\s+")
    }
}
