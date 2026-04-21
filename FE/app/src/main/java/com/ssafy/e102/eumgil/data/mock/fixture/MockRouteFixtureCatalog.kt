package com.ssafy.e102.eumgil.data.mock.fixture

import com.ssafy.e102.eumgil.data.route.RouteDto
import com.ssafy.e102.eumgil.data.route.RoutePointDto
import com.ssafy.e102.eumgil.data.route.RouteSearchRequestDto
import com.ssafy.e102.eumgil.data.route.RouteSearchResponseDto
import com.ssafy.e102.eumgil.data.route.RouteSegmentDto
import java.util.Locale

object MockRouteFixtureCatalog {
    val defaultFixture: RouteFixtureTemplate =
        RouteFixtureTemplate(
            fixtureId = "busan-cityhall-to-station-demo",
            name = "Busan City Hall to Busan Station demo route",
            routes =
                listOf(
                    RouteFixtureRouteTemplate(
                        routeOption = "SAFE",
                        title = "Safe Route",
                        distanceMeter = 980,
                        estimatedTimeMinute = 16,
                        riskLevel = "LOW",
                        segments =
                            listOf(
                                RouteFixtureSegmentTemplate(
                                    sequence = 1,
                                    geometryPoints =
                                        listOf(
                                            RouteFixtureGeometryPointTemplate(progress = 0.00),
                                            RouteFixtureGeometryPointTemplate(
                                                progress = 0.20,
                                                latOffset = 0.00018,
                                                lngOffset = 0.00022,
                                            ),
                                            RouteFixtureGeometryPointTemplate(
                                                progress = 0.52,
                                                latOffset = 0.00034,
                                                lngOffset = 0.00030,
                                            ),
                                        ),
                                    distanceMeter = 410,
                                    hasCrosswalk = true,
                                    hasSignal = true,
                                    hasAudioSignal = true,
                                    hasBrailleBlock = true,
                                    riskLevel = "LOW",
                                    guidanceMessage = "Head to the audio-signaled crosswalk and keep straight.",
                                ),
                                RouteFixtureSegmentTemplate(
                                    sequence = 2,
                                    geometryPoints =
                                        listOf(
                                            RouteFixtureGeometryPointTemplate(
                                                progress = 0.52,
                                                latOffset = 0.00034,
                                                lngOffset = 0.00030,
                                            ),
                                            RouteFixtureGeometryPointTemplate(
                                                progress = 0.82,
                                                latOffset = 0.00014,
                                                lngOffset = 0.00012,
                                            ),
                                        ),
                                    distanceMeter = 310,
                                    hasStairs = false,
                                    hasCurbGap = false,
                                    hasCrosswalk = false,
                                    hasBrailleBlock = true,
                                    riskLevel = "LOW",
                                    guidanceMessage = "Follow the braille block guidance line along the sidewalk.",
                                ),
                                RouteFixtureSegmentTemplate(
                                    sequence = 3,
                                    geometryPoints =
                                        listOf(
                                            RouteFixtureGeometryPointTemplate(
                                                progress = 0.82,
                                                latOffset = 0.00014,
                                                lngOffset = 0.00012,
                                            ),
                                            RouteFixtureGeometryPointTemplate(progress = 1.00),
                                        ),
                                    distanceMeter = 260,
                                    hasCrosswalk = true,
                                    hasSignal = true,
                                    riskLevel = "LOW",
                                    guidanceMessage = "Cross once more and arrive at the destination entrance.",
                                ),
                            ),
                    ),
                    RouteFixtureRouteTemplate(
                        routeOption = "SHORTEST",
                        title = "Shortest Route",
                        distanceMeter = 820,
                        estimatedTimeMinute = 13,
                        riskLevel = "MEDIUM",
                        segments =
                            listOf(
                                RouteFixtureSegmentTemplate(
                                    sequence = 1,
                                    geometryPoints =
                                        listOf(
                                            RouteFixtureGeometryPointTemplate(progress = 0.00),
                                            RouteFixtureGeometryPointTemplate(
                                                progress = 0.48,
                                                latOffset = -0.00008,
                                                lngOffset = -0.00012,
                                            ),
                                        ),
                                    distanceMeter = 360,
                                    hasCrosswalk = true,
                                    hasSignal = false,
                                    hasAudioSignal = false,
                                    riskLevel = "MEDIUM",
                                    guidanceMessage = "Use the shortest crossing route and prepare for a curb gap.",
                                ),
                                RouteFixtureSegmentTemplate(
                                    sequence = 2,
                                    geometryPoints =
                                        listOf(
                                            RouteFixtureGeometryPointTemplate(
                                                progress = 0.48,
                                                latOffset = -0.00008,
                                                lngOffset = -0.00012,
                                            ),
                                            RouteFixtureGeometryPointTemplate(progress = 1.00),
                                        ),
                                    distanceMeter = 460,
                                    hasCurbGap = true,
                                    hasCrosswalk = false,
                                    hasBrailleBlock = false,
                                    riskLevel = "MEDIUM",
                                    guidanceMessage = "Continue on the direct sidewalk stretch to the destination.",
                                ),
                            ),
                    ),
                ),
        )
}

data class RouteFixtureTemplate(
    val fixtureId: String,
    val name: String,
    val routes: List<RouteFixtureRouteTemplate>,
) {
    init {
        require(fixtureId.isNotBlank()) { "Route fixture id must not be blank." }
        require(name.isNotBlank()) { "Route fixture name must not be blank." }
        require(routes.isNotEmpty()) { "Route fixture requires at least one route template." }
        require(routes.map(RouteFixtureRouteTemplate::normalizedRouteOption).distinct().size == routes.size) {
            "Route fixture route options must be unique."
        }
    }

    fun resolve(request: RouteSearchRequestDto): RouteSearchResponseDto {
        val requestedOptions = request.normalizedRouteOptions()

        val resolvedRoutes =
            routes
                .filter { route ->
                    requestedOptions.isEmpty() || route.normalizedRouteOption() in requestedOptions
                }.map { route ->
                    route.toDto(request)
                }

        return RouteSearchResponseDto(routes = resolvedRoutes)
    }
}

data class RouteFixtureRouteTemplate(
    val routeOption: String,
    val title: String,
    val distanceMeter: Int,
    val estimatedTimeMinute: Int,
    val riskLevel: String,
    val segments: List<RouteFixtureSegmentTemplate>,
) {
    init {
        require(routeOption.isNotBlank()) { "Route fixture route option must not be blank." }
        require(title.isNotBlank()) { "Route fixture route title must not be blank." }
        require(distanceMeter >= 0) { "Route fixture route distance must be non-negative." }
        require(estimatedTimeMinute >= 0) { "Route fixture route estimated time must be non-negative." }
        require(segments.isNotEmpty()) { "Route fixture route requires at least one segment." }
    }

    fun normalizedRouteOption(): String = routeOption.trim().uppercase(Locale.US)

    fun toDto(request: RouteSearchRequestDto): RouteDto =
        RouteDto(
            routeOption = normalizedRouteOption(),
            title = title,
            distanceMeter = distanceMeter,
            estimatedTimeMinute = estimatedTimeMinute,
            riskLevel = riskLevel,
            segments =
                segments.map { segment ->
                    segment.toDto(request)
                },
        )
}

data class RouteFixtureSegmentTemplate(
    val sequence: Int,
    val geometryPoints: List<RouteFixtureGeometryPointTemplate>,
    val distanceMeter: Int,
    val hasStairs: Boolean = false,
    val hasCurbGap: Boolean = false,
    val hasCrosswalk: Boolean = false,
    val hasSignal: Boolean = false,
    val hasAudioSignal: Boolean = false,
    val hasBrailleBlock: Boolean = false,
    val riskLevel: String,
    val guidanceMessage: String,
) {
    init {
        require(sequence > 0) { "Route fixture segment sequence must be positive." }
        require(geometryPoints.size >= 2) { "Route fixture segment requires at least two geometry points." }
        require(distanceMeter >= 0) { "Route fixture segment distance must be non-negative." }
        require(riskLevel.isNotBlank()) { "Route fixture segment risk level must not be blank." }
        require(guidanceMessage.isNotBlank()) { "Route fixture segment guidance must not be blank." }
    }

    fun toDto(request: RouteSearchRequestDto): RouteSegmentDto =
        RouteSegmentDto(
            sequence = sequence,
            geometry = geometryPoints.toLinestring(start = request.startPoint, end = request.endPoint),
            distanceMeter = distanceMeter,
            hasStairs = hasStairs,
            hasCurbGap = hasCurbGap,
            hasCrosswalk = hasCrosswalk,
            hasSignal = hasSignal,
            hasAudioSignal = hasAudioSignal,
            hasBrailleBlock = hasBrailleBlock,
            riskLevel = riskLevel,
            guidanceMessage = guidanceMessage,
        )
}

data class RouteFixtureGeometryPointTemplate(
    val progress: Double,
    val latOffset: Double = 0.0,
    val lngOffset: Double = 0.0,
) {
    init {
        require(progress in 0.0..1.0) { "Route fixture geometry point progress must be between 0.0 and 1.0." }
    }

    fun resolve(
        start: RoutePointDto,
        end: RoutePointDto,
    ): RoutePointDto =
        RoutePointDto(
            lat = start.lat + ((end.lat - start.lat) * progress) + latOffset,
            lng = start.lng + ((end.lng - start.lng) * progress) + lngOffset,
        )
}

private fun RouteSearchRequestDto.normalizedRouteOptions(): Set<String> =
    routeOptions
        .map(String::trim)
        .filter(String::isNotBlank)
        .map { routeOption -> routeOption.uppercase(Locale.US) }
        .toSet()

private fun List<RouteFixtureGeometryPointTemplate>.toLinestring(
    start: RoutePointDto,
    end: RoutePointDto,
): String =
    joinToString(
        prefix = "LINESTRING(",
        postfix = ")",
        separator = ", ",
    ) { pointTemplate ->
        val point = pointTemplate.resolve(start = start, end = end)
        "${point.lng.toGeometryValue()} ${point.lat.toGeometryValue()}"
    }

private fun Double.toGeometryValue(): String = String.format(Locale.US, "%.6f", this)
