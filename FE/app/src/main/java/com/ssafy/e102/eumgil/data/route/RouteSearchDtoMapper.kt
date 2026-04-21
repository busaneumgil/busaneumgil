package com.ssafy.e102.eumgil.data.route

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteDefaults
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RoutePolyline
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSearchResult
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteSegmentSafetyFlags
import com.ssafy.e102.eumgil.core.model.RouteSummary
import kotlin.math.ceil

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
    val segments =
        segments
            .mapIndexed { index, segment ->
                segment.toDomain(
                    fallbackSequence = index + 1,
                    geometryParser = geometryParser,
                )
            }.sortedBy(RouteSegment::sequence)
    val segmentDistanceTotal = segments.sumOf(RouteSegment::distanceMeters)
    val previewPolyline = segments.toPreviewPolyline()
    val distanceMeters = normalizedDistance(segmentDistanceTotal)

    return RouteCandidate(
        routeOption = resolvedOption,
        title = normalizedTitle().ifEmpty { defaultTitle(resolvedOption) },
        summary =
            RouteSummary(
                distanceMeters = distanceMeters,
                estimatedTimeMinutes = normalizedEstimatedTime(distanceMeters = distanceMeters),
                riskLevel = RouteRiskLevel.fromValue(riskLevel, fallback = segments.maxRiskLevel()),
            ),
        previewPolyline = previewPolyline,
        segments = segments,
    )
}

private fun RouteSegmentDto.toDomain(
    fallbackSequence: Int,
    geometryParser: RouteGeometryParser,
): RouteSegment =
    RouteSegment(
        sequence = normalizedSequence(fallbackSequence),
        polyline = geometryParser.parse(geometry).polyline,
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

private fun defaultTitle(routeOption: RouteOption): String =
    when (routeOption) {
        RouteOption.SAFE -> "Safe Route"
        RouteOption.SHORTEST -> "Shortest Route"
    }

private fun List<RouteSegment>.toPreviewPolyline(): RoutePolyline {
    val previewPoints = mutableListOf<GeoCoordinate>()

    forEach { segment ->
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

private const val DEFAULT_WALKING_SPEED_METERS_PER_MINUTE: Double = 60.0
