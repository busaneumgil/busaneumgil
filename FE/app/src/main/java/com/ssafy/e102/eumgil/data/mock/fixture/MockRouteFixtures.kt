package com.ssafy.e102.eumgil.data.mock.fixture

import com.ssafy.e102.eumgil.data.route.RouteDto
import com.ssafy.e102.eumgil.data.route.RoutePointDto
import com.ssafy.e102.eumgil.data.route.RouteSearchRequestDto
import com.ssafy.e102.eumgil.data.route.RouteSearchResponseDto
import com.ssafy.e102.eumgil.data.route.RouteSegmentDto
import java.util.Locale

object MockRouteFixtures {
    fun searchRoutes(request: RouteSearchRequestDto): RouteSearchResponseDto {
        val requestedOptions =
            request.routeOptions
                .ifEmpty { listOf(SAFE_OPTION, SHORTEST_OPTION) }
                .map(String::trim)
                .map(String::uppercase)

        val routes =
            requestedOptions.mapNotNull { routeOption ->
                when (routeOption) {
                    SAFE_OPTION -> safeRoute(request)
                    SHORTEST_OPTION -> shortestRoute(request)
                    else -> null
                }
            }

        return RouteSearchResponseDto(routes = routes)
    }

    private fun safeRoute(request: RouteSearchRequestDto): RouteDto {
        val start = request.startPoint
        val end = request.endPoint
        val safeMid1 = interpolatePoint(start = start, end = end, progress = 0.20, latOffset = 0.00018, lngOffset = 0.00022)
        val safeMid2 = interpolatePoint(start = start, end = end, progress = 0.52, latOffset = 0.00034, lngOffset = 0.00030)
        val safeMid3 = interpolatePoint(start = start, end = end, progress = 0.82, latOffset = 0.00014, lngOffset = 0.00012)

        return RouteDto(
            routeOption = SAFE_OPTION,
            title = "Safe Route",
            distanceMeter = 980,
            estimatedTimeMinute = 16,
            riskLevel = "LOW",
            segments =
                listOf(
                    RouteSegmentDto(
                        sequence = 1,
                        geometry = linestring(start, safeMid1, safeMid2),
                        distanceMeter = 410,
                        hasCrosswalk = true,
                        hasSignal = true,
                        hasAudioSignal = true,
                        hasBrailleBlock = true,
                        riskLevel = "LOW",
                        guidanceMessage = "Head to the audio-signaled crosswalk and keep straight.",
                    ),
                    RouteSegmentDto(
                        sequence = 2,
                        geometry = linestring(safeMid2, safeMid3),
                        distanceMeter = 310,
                        hasStairs = false,
                        hasCurbGap = false,
                        hasCrosswalk = false,
                        hasBrailleBlock = true,
                        riskLevel = "LOW",
                        guidanceMessage = "Follow the braille block guidance line along the sidewalk.",
                    ),
                    RouteSegmentDto(
                        sequence = 3,
                        geometry = linestring(safeMid3, end),
                        distanceMeter = 260,
                        hasCrosswalk = true,
                        hasSignal = true,
                        riskLevel = "LOW",
                        guidanceMessage = "Cross once more and arrive at the destination entrance.",
                    ),
                ),
        )
    }

    private fun shortestRoute(request: RouteSearchRequestDto): RouteDto {
        val start = request.startPoint
        val end = request.endPoint
        val shortestMid = interpolatePoint(start = start, end = end, progress = 0.48, latOffset = -0.00008, lngOffset = -0.00012)

        return RouteDto(
            routeOption = SHORTEST_OPTION,
            title = "Shortest Route",
            distanceMeter = 820,
            estimatedTimeMinute = 13,
            riskLevel = "MEDIUM",
            segments =
                listOf(
                    RouteSegmentDto(
                        sequence = 1,
                        geometry = linestring(start, shortestMid),
                        distanceMeter = 360,
                        hasCrosswalk = true,
                        hasSignal = false,
                        hasAudioSignal = false,
                        riskLevel = "MEDIUM",
                        guidanceMessage = "Use the shortest crossing route and prepare for a curb gap.",
                    ),
                    RouteSegmentDto(
                        sequence = 2,
                        geometry = linestring(shortestMid, end),
                        distanceMeter = 460,
                        hasCurbGap = true,
                        hasCrosswalk = false,
                        hasBrailleBlock = false,
                        riskLevel = "MEDIUM",
                        guidanceMessage = "Continue on the direct sidewalk stretch to the destination.",
                    ),
                ),
        )
    }

    private fun interpolatePoint(
        start: RoutePointDto,
        end: RoutePointDto,
        progress: Double,
        latOffset: Double = 0.0,
        lngOffset: Double = 0.0,
    ): RoutePointDto =
        RoutePointDto(
            lat = start.lat + ((end.lat - start.lat) * progress) + latOffset,
            lng = start.lng + ((end.lng - start.lng) * progress) + lngOffset,
        )

    private fun linestring(vararg points: RoutePointDto): String =
        points.joinToString(
            prefix = "LINESTRING(",
            postfix = ")",
            separator = ", ",
        ) { point ->
            "${point.lng.toGeometryValue()} ${point.lat.toGeometryValue()}"
        }

    private fun Double.toGeometryValue(): String = String.format(Locale.US, "%.6f", this)

    private const val SAFE_OPTION: String = "SAFE"
    private const val SHORTEST_OPTION: String = "SHORTEST"
}
