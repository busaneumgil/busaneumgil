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

    val hasFallbackSegments: Boolean
        get() = fallbackSegmentCount > 0

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

    val renderableSegments: List<RouteSegment>
        get() = segments.filter(RouteSegment::hasRenderablePolyline)

    val hasFallbackSegments: Boolean
        get() = preview.hasFallbackSegments
}

data class RouteSearchResult(
    val origin: RouteWaypoint,
    val destination: RouteWaypoint,
    val routes: List<RouteCandidate> = emptyList(),
) {
    val primaryRoute: RouteCandidate?
        get() = routes.firstOrNull()

    val availableOptions: List<RouteOption>
        get() = routes.map(RouteCandidate::routeOption)

    val renderableRoutes: List<RouteCandidate>
        get() = routes.filter(RouteCandidate::hasRenderablePreview)

    fun findRoute(routeOption: RouteOption): RouteCandidate? =
        routes.firstOrNull { route -> route.routeOption == routeOption }
}

enum class RouteSearchSourceType {
    MOCK_FIXTURE,
}

data class RouteSearchSource(
    val type: RouteSearchSourceType,
    val label: String,
    val fixtureId: String? = null,
    val isFromCache: Boolean = false,
) {
    init {
        require(label.isNotBlank()) { "Route search source label must not be blank." }
    }

    fun asCached(): RouteSearchSource = copy(isFromCache = true)

    companion object {
        fun mockFixture(
            fixtureId: String,
            label: String,
            isFromCache: Boolean = false,
        ): RouteSearchSource =
            RouteSearchSource(
                type = RouteSearchSourceType.MOCK_FIXTURE,
                label = label,
                fixtureId = fixtureId,
                isFromCache = isFromCache,
            )
    }
}

data class RouteSearchData(
    val query: RouteSearchQuery,
    val result: RouteSearchResult,
    val source: RouteSearchSource,
) {
    val routes: List<RouteCandidate>
        get() = result.routes

    val primaryRoute: RouteCandidate?
        get() = result.primaryRoute

    val availableOptions: List<RouteOption>
        get() = result.availableOptions

    val renderableRoutes: List<RouteCandidate>
        get() = result.renderableRoutes

    fun findRoute(routeOption: RouteOption): RouteCandidate? = result.findRoute(routeOption)
}

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
