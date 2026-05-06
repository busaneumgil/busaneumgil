package com.ssafy.e102.eumgil.core.model

data class RouteBookmark(
    val bookmarkId: String,
    val routeName: String,
    val startLabel: String,
    val endLabel: String,
    val startPoint: GeoCoordinate,
    val endPoint: GeoCoordinate,
    val routeOption: RouteOption,
    val distanceMeters: Int? = null,
    val durationMinutes: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class RouteBookmarkDraft(
    val startLabel: String,
    val endLabel: String,
    val startPoint: GeoCoordinate,
    val endPoint: GeoCoordinate,
    val routeOption: RouteOption,
    val distanceMeters: Int? = null,
    val durationMinutes: Int? = null,
) {
    val defaultRouteName: String
        get() = "${startLabel.orDefaultStartLabel()}-${endLabel.orDefaultEndLabel()}"

    fun toSaveRequest(routeName: String = defaultRouteName): RouteBookmarkSaveRequest =
        RouteBookmarkSaveRequest(
            routeName = routeName.trim().ifBlank { defaultRouteName },
            startLabel = startLabel.orDefaultStartLabel(),
            endLabel = endLabel.orDefaultEndLabel(),
            startPoint = startPoint,
            endPoint = endPoint,
            routeOption = routeOption,
            distanceMeters = distanceMeters,
            durationMinutes = durationMinutes,
        )
}

data class RouteBookmarkSaveRequest(
    val routeName: String,
    val startLabel: String,
    val endLabel: String,
    val startPoint: GeoCoordinate,
    val endPoint: GeoCoordinate,
    val routeOption: RouteOption,
    val distanceMeters: Int? = null,
    val durationMinutes: Int? = null,
)

private fun String.orDefaultStartLabel(): String = trim().ifBlank { DEFAULT_ROUTE_BOOKMARK_START_LABEL }

private fun String.orDefaultEndLabel(): String = trim().ifBlank { DEFAULT_ROUTE_BOOKMARK_END_LABEL }

private const val DEFAULT_ROUTE_BOOKMARK_START_LABEL = "출발지"
private const val DEFAULT_ROUTE_BOOKMARK_END_LABEL = "도착지"
