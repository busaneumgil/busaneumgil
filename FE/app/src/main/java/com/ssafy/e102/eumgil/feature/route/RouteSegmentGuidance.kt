package com.ssafy.e102.eumgil.feature.route

import com.ssafy.e102.eumgil.core.model.RouteSegment
import java.util.Locale

internal fun RouteSegment.toRouteDetailStepKind(): RouteDetailStepKind {
    val normalizedGuidance = guidanceMessage.trim().lowercase(Locale.US)

    return when {
        safetyFlags.hasStairs -> RouteDetailStepKind.STAIRS
        safetyFlags.hasCurbGap -> RouteDetailStepKind.CURB_GAP
        normalizedGuidance.containsAnyRouteGuidanceKeyword("엘리베이터", "elevator", "lift") ->
            RouteDetailStepKind.ELEVATOR
        normalizedGuidance.containsAnyRouteGuidanceKeyword("공사", "construction", "우회", "narrow path") ->
            RouteDetailStepKind.CONSTRUCTION
        safetyFlags.hasCrosswalk -> RouteDetailStepKind.CROSSWALK
        normalizedGuidance.containsAnyRouteGuidanceKeyword("좌회전", "왼쪽", "turn left", "left turn") ->
            RouteDetailStepKind.TURN_LEFT
        normalizedGuidance.containsAnyRouteGuidanceKeyword("우회전", "오른쪽", "turn right", "right turn") ->
            RouteDetailStepKind.TURN_RIGHT
        safetyFlags.hasBrailleBlock -> RouteDetailStepKind.TACTILE_GUIDE
        else -> RouteDetailStepKind.STRAIGHT
    }
}

private fun String.containsAnyRouteGuidanceKeyword(vararg keywords: String): Boolean =
    keywords.any { keyword -> contains(keyword, ignoreCase = true) }
