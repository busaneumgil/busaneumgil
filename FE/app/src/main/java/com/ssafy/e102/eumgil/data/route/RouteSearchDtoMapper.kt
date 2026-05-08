package com.ssafy.e102.eumgil.data.route

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteDefaults
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RoutePolyline
import com.ssafy.e102.eumgil.core.model.RoutePreviewModel
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSearchResult
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteSegmentSafetyFlags
import com.ssafy.e102.eumgil.core.model.RouteSummary
import kotlin.math.ceil
import kotlin.math.roundToInt

fun RouteSearchQuery.toRequestDto(): RouteSearchRequestDto =
    RouteSearchRequestDto(
        startPoint = origin.coordinate.toPointDto(),
        endPoint = destination.coordinate.toPointDto(),
        routeOptions = requestedOptions.map(RouteOption::name),
    )

fun RouteSearchResponseDto.toDomain(
    query: RouteSearchQuery,
    geometryParser: RouteGeometryParser,
): RouteSearchResult =
    RouteSearchResult(
        origin = query.origin,
        destination = query.destination,
        routes =
            routes.mapIndexed { index, route ->
                route.toDomain(
                    defaultOption = query.requestedOptions.getOrElse(index) { RouteOption.SAFE },
                    geometryParser = geometryParser,
                )
            },
    )

private fun RouteDto.toDomain(
    defaultOption: RouteOption,
    geometryParser: RouteGeometryParser,
): RouteCandidate {
    val resolvedOption = RouteOption.fromValue(routeOption) ?: defaultOption
    val sourceSegments =
        segments.ifEmpty {
            legs.toSegmentDtos()
        }
    val parsedSegments = sourceSegments.toParsedSegments(geometryParser = geometryParser)
    val segmentDistanceTotal = parsedSegments.segments.sumOf(RouteSegment::distanceMeters)
    val distanceMeters = normalizedDistance(segmentDistanceTotal)

    return RouteCandidate(
        routeOption = resolvedOption,
        title = normalizedTitle().ifEmpty { defaultTitle(resolvedOption) },
        summary =
            RouteSummary(
                distanceMeters = distanceMeters,
                estimatedTimeMinutes = normalizedEstimatedTime(distanceMeters = distanceMeters),
                riskLevel = RouteRiskLevel.fromValue(riskLevel, fallback = parsedSegments.segments.maxRiskLevel()),
            ),
        preview = parsedSegments.preview,
        segments = parsedSegments.segments,
    )
}

private fun RouteSegmentDto.toDomain(
    fallbackSequence: Int,
    geometryParser: RouteGeometryParser,
): RouteSegment {
    val geometryParseResult = geometryParser.parse(geometry)

    return RouteSegment(
        sequence = normalizedSequence(fallbackSequence),
        polyline = geometryParseResult.polyline,
        distanceMeters = normalizedDistance(),
        safetyFlags =
            RouteSegmentSafetyFlags(
                hasStairs = hasStairs == true,
                hasCurbGap = hasCurbGap == true,
                hasCrosswalk = hasCrosswalk == true,
                hasSignal = hasSignal == true,
                hasAudioSignal = hasAudioSignal == true,
                hasBrailleBlock = hasBrailleBlock == true,
        ),
        riskLevel = RouteRiskLevel.fromValue(riskLevel),
        guidanceMessage = normalizedGuidanceMessage(),
    )
}

private fun GeoCoordinate.toPointDto(): RoutePointDto =
    RoutePointDto(
        lat = latitude,
        lng = longitude,
    )

private fun RouteDto.normalizedTitle(): String = title?.trim().orEmpty()

private fun RouteDto.normalizedDistance(segmentDistanceTotal: Int): Int =
    distanceMeter
        ?.takeIf { distance -> distance >= 0 }
        ?: segmentDistanceTotal

private fun RouteDto.normalizedEstimatedTime(distanceMeters: Int): Int =
    estimatedTimeMinute
        ?.takeIf { estimatedTime -> estimatedTime >= 0 }
        ?: distanceMeters.toEstimatedMinutes()

private fun RouteSegmentDto.normalizedSequence(fallbackSequence: Int): Int =
    sequence
        ?.takeIf { candidateSequence -> candidateSequence > 0 }
        ?: fallbackSequence

private fun RouteSegmentDto.normalizedDistance(): Int =
    distanceMeter
        ?.takeIf { distance -> distance >= 0 }
        ?: 0

private fun RouteSegmentDto.normalizedGuidanceMessage(): String =
    guidanceMessage
        ?.trim()
        .orEmpty()
        .ifEmpty { RouteDefaults.DEFAULT_GUIDANCE_MESSAGE }

private fun List<RouteLegDto>.toSegmentDtos(): List<RouteSegmentDto> =
    flatMap { leg -> leg.steps }
        .mapIndexed { index, step -> step.toSegmentDto(fallbackSequence = index + 1) }

private fun RouteStepDto.toSegmentDto(fallbackSequence: Int): RouteSegmentDto {
    val alertType = RouteStepAlertType.fromValue(alert?.type)
    return RouteSegmentDto(
        sequence = sequence?.takeIf { candidateSequence -> candidateSequence > 0 } ?: fallbackSequence,
        geometry = geometry,
        distanceMeter = distanceMeter?.takeIf { distance -> distance >= 0.0 }?.roundToInt(),
        hasStairs = alertType == RouteStepAlertType.STAIR,
        hasCrosswalk = alertType?.isCrosswalk == true,
        hasSignal = alertType == RouteStepAlertType.CROSSWALK_SIGNAL ||
            alertType == RouteStepAlertType.CROSSWALK_AUDIO,
        hasAudioSignal = alertType == RouteStepAlertType.CROSSWALK_AUDIO,
        riskLevel = alertType?.riskLevel,
        guidanceMessage = instruction,
    )
}

private fun defaultTitle(routeOption: RouteOption): String =
    when (routeOption) {
        RouteOption.SAFE -> "Safe Route"
        RouteOption.SHORTEST -> "Shortest Route"
    }

fun List<RouteSegmentDto>.toRoutePreviewModel(geometryParser: RouteGeometryParser): RoutePreviewModel =
    toParsedSegments(geometryParser = geometryParser).preview

private fun List<RouteSegmentDto>.toParsedSegments(geometryParser: RouteGeometryParser): ParsedRouteSegments {
    val parsedSegments =
        mapIndexed { index, segment ->
            segment.toDomain(
                fallbackSequence = index + 1,
                geometryParser = geometryParser,
            )
        }.sortedBy(RouteSegment::sequence)

    val renderableSegmentCount = parsedSegments.count(RouteSegment::hasRenderablePolyline)
    return ParsedRouteSegments(
        segments = parsedSegments,
        preview =
            RoutePreviewModel(
                polyline = parsedSegments.toPreviewPolyline(),
                segmentCount = parsedSegments.size,
                renderableSegmentCount = renderableSegmentCount,
                fallbackSegmentCount = parsedSegments.size - renderableSegmentCount,
            ),
    )
}

private fun List<RouteSegment>.toPreviewPolyline(): RoutePolyline {
    val previewPoints = mutableListOf<GeoCoordinate>()

    forEach { segment ->
        if (!segment.hasRenderablePolyline) return@forEach

        segment.polyline.points.forEach { point ->
            if (previewPoints.lastOrNull() != point) {
                previewPoints += point
            }
        }
    }

    return RoutePolyline(points = previewPoints)
}

private fun List<RouteSegment>.maxRiskLevel(): RouteRiskLevel =
    maxByOrNull { segment -> segment.riskLevel.severity }
        ?.riskLevel
        ?: RouteRiskLevel.MEDIUM

private val RouteRiskLevel.severity: Int
    get() =
        when (this) {
            RouteRiskLevel.LOW -> 0
            RouteRiskLevel.MEDIUM -> 1
            RouteRiskLevel.HIGH -> 2
        }

private fun Int.toEstimatedMinutes(): Int =
    if (this <= 0) {
        0
    } else {
        ceil(this / DEFAULT_WALKING_SPEED_METERS_PER_MINUTE).toInt()
    }

private data class ParsedRouteSegments(
    val segments: List<RouteSegment>,
    val preview: RoutePreviewModel,
)

private const val DEFAULT_WALKING_SPEED_METERS_PER_MINUTE: Double = 60.0

private enum class RouteStepAlertType(
    val isCrosswalk: Boolean = false,
    val riskLevel: String? = null,
) {
    CROSSWALK(isCrosswalk = true),
    CROSSWALK_SIGNAL(isCrosswalk = true),
    CROSSWALK_AUDIO(isCrosswalk = true),
    STAIR(riskLevel = "HIGH"),
    NARROW_SIDEWALK(riskLevel = "HIGH"),
    UNPAVED(riskLevel = "MEDIUM"),
    MIDDLE_SLOPE(riskLevel = "MEDIUM"),
    ELEVATOR,
    BUS_STOP,
    SUBWAY_ELEVATOR,
    ALIGHTING_POINT,
    ;

    companion object {
        fun fromValue(value: String?): RouteStepAlertType? =
            entries.firstOrNull { type ->
                type.name.equals(value?.trim(), ignoreCase = true)
            }
    }
}
