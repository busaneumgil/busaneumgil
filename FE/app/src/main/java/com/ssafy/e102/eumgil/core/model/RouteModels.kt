package com.ssafy.e102.eumgil.core.model

data class RouteWaypoint(
    val name: String? = null,
    val placeId: String? = null,
    val address: String? = null,
    val coordinate: GeoCoordinate,
)

data class RouteSearchQuery(
    val origin: RouteWaypoint,
    val destination: RouteWaypoint,
    val requestedOptions: List<RouteOption> = RouteOption.defaultSearchOptions,
) {
    init {
        require(requestedOptions.isNotEmpty()) { "Route search query requires at least one route option." }
        require(requestedOptions.distinct().size == requestedOptions.size) {
            "Route search query options must be unique."
        }
    }
}

enum class RouteOption {
    SAFE,
    SHORTEST,
    ;

    companion object {
        val defaultSearchOptions: List<RouteOption> = listOf(SAFE, SHORTEST)

        fun fromValue(value: String?): RouteOption? =
            entries.firstOrNull { option ->
                option.name.equals(value?.trim(), ignoreCase = true)
            }
    }
}

enum class RouteRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    ;

    companion object {
        fun fromValue(
            value: String?,
            fallback: RouteRiskLevel = MEDIUM,
        ): RouteRiskLevel =
            entries.firstOrNull { level ->
                level.name.equals(value?.trim(), ignoreCase = true)
            } ?: fallback
    }
}

data class RouteSummary(
    val distanceMeters: Int,
    val estimatedTimeMinutes: Int,
    val riskLevel: RouteRiskLevel,
)

data class RoutePolyline(
    val points: List<GeoCoordinate> = emptyList(),
) {
    val isRenderable: Boolean
        get() = points.size >= 2

    val start: GeoCoordinate?
        get() = points.firstOrNull()

    val end: GeoCoordinate?
        get() = points.lastOrNull()
}

data class RoutePreviewModel(
    val polyline: RoutePolyline = RoutePolyline(),
    val segmentCount: Int = 0,
    val renderableSegmentCount: Int = 0,
    val fallbackSegmentCount: Int = 0,
) {
    val hasRenderableLine: Boolean
        get() = polyline.isRenderable

    val skippedSegmentCount: Int
        get() = (segmentCount - renderableSegmentCount).coerceAtLeast(0)
}

data class RouteSegmentSafetyFlags(
    val hasStairs: Boolean = false,
    val hasCurbGap: Boolean = false,
    val hasCrosswalk: Boolean = false,
    val hasSignal: Boolean = false,
    val hasAudioSignal: Boolean = false,
    val hasBrailleBlock: Boolean = false,
)

data class RouteSegment(
    val sequence: Int,
    val polyline: RoutePolyline = RoutePolyline(),
    val distanceMeters: Int = 0,
    val safetyFlags: RouteSegmentSafetyFlags = RouteSegmentSafetyFlags(),
    val riskLevel: RouteRiskLevel = RouteRiskLevel.MEDIUM,
    val guidanceMessage: String = RouteDefaults.DEFAULT_GUIDANCE_MESSAGE,
) {
    val hasRenderablePolyline: Boolean
        get() = polyline.isRenderable
}

data class RouteCandidate(
    val routeOption: RouteOption,
    val title: String,
    val summary: RouteSummary,
    val preview: RoutePreviewModel = RoutePreviewModel(),
    val segments: List<RouteSegment> = emptyList(),
) {
    val previewPolyline: RoutePolyline
        get() = preview.polyline

    val hasRenderablePreview: Boolean
        get() = preview.hasRenderableLine
}

data class RouteSearchResult(
    val origin: RouteWaypoint,
    val destination: RouteWaypoint,
    val routes: List<RouteCandidate> = emptyList(),
)

object RouteDefaults {
    const val DEFAULT_GUIDANCE_MESSAGE: String = "Continue on the suggested route."
}

fun PlaceDestination.toRouteWaypoint(): RouteWaypoint =
    RouteWaypoint(
        name = name,
        placeId = placeId,
        address = address,
        coordinate =
            GeoCoordinate(
                latitude = latitude,
                longitude = longitude,
            ),
    )
