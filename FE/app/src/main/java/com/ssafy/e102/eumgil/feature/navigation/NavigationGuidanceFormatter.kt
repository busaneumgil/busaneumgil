package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteSegment

internal data class NavigationBriefingItem(
    val sequence: Int,
    val instruction: String,
    val guidanceAction: NavigationGuidanceAction,
)

internal fun RouteCandidate.toNavigationBriefingItems(): List<NavigationBriefingItem> =
    segments.mapIndexed { index, segment ->
        NavigationBriefingItem(
            sequence = index + 1,
            instruction = segment.toCompactNavigationInstruction(),
            guidanceAction = segment.toNavigationGuidanceAction(),
        )
    }

internal fun RouteSegment.toCompactNavigationInstruction(): String {
    val action = guidanceMessage.toCompactNavigationAction()
    if (action == "도착") return action

    val distance = distanceMeters.toCompactNavigationDistance()
    return if (distance.isBlank()) {
        action
    } else {
        "$distance 후 $action"
    }
}

private fun String.toCompactNavigationAction(): String {
    val message = trim().lowercase()
    return when {
        message.contains("도착") -> "도착"
        message.contains("우회전") ||
            message.contains("오른쪽") ||
            message.contains("right") -> "우회전"
        message.contains("좌회전") ||
            message.contains("왼쪽") ||
            message.contains("left") -> "좌회전"
        message.contains("직진") ||
            message.contains("straight") ||
            message.contains("continue") -> "직진"
        message.contains("횡단") ||
            message.contains("cross") -> "횡단"
        message.contains("지하도") -> "지하도"
        else -> "이동"
    }
}

private fun Int.toCompactNavigationDistance(): String =
    when {
        this <= 0 -> ""
        this < 1_000 -> "${this}m"
        this % 1_000 == 0 -> "${this / 1_000}km"
        else -> String.format(java.util.Locale.US, "%.1fkm", this / 1_000.0)
    }
