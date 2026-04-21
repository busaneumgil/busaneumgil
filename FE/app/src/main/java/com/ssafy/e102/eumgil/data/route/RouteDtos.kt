package com.ssafy.e102.eumgil.data.route

data class RouteSearchRequestDto(
    val startPoint: RoutePointDto,
    val endPoint: RoutePointDto,
    val routeOptions: List<String> = emptyList(),
)

data class RoutePointDto(
    val lat: Double,
    val lng: Double,
)

data class RouteSearchResponseDto(
    val routes: List<RouteDto> = emptyList(),
)

data class RouteDto(
    val routeOption: String? = null,
    val title: String? = null,
    val distanceMeter: Int? = null,
    val estimatedTimeMinute: Int? = null,
    val riskLevel: String? = null,
    val segments: List<RouteSegmentDto> = emptyList(),
)

data class RouteSegmentDto(
    val sequence: Int? = null,
    val geometry: String? = null,
    val distanceMeter: Int? = null,
    val hasStairs: Boolean? = null,
    val hasCurbGap: Boolean? = null,
    val hasCrosswalk: Boolean? = null,
    val hasSignal: Boolean? = null,
    val hasAudioSignal: Boolean? = null,
    val hasBrailleBlock: Boolean? = null,
    val riskLevel: String? = null,
    val guidanceMessage: String? = null,
)
