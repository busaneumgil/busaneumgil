package com.ssafy.e102.eumgil.feature.route

import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteGuidanceDirection
import com.ssafy.e102.eumgil.core.model.RouteLeg
import com.ssafy.e102.eumgil.core.model.RouteLegType
import com.ssafy.e102.eumgil.core.model.RouteSegment
import java.util.Locale

internal fun RouteCandidate.toRouteDetailStepKind(segment: RouteSegment): RouteDetailStepKind =
    when (segment.resolveSourceLeg(legs = legs)?.type) {
        RouteLegType.BUS -> RouteDetailStepKind.BUS
        RouteLegType.SUBWAY -> RouteDetailStepKind.SUBWAY
        else -> segment.toRouteDetailStepKind()
    }

internal fun RouteSegment.toRouteDetailStepKind(): RouteDetailStepKind {
    val normalizedGuidance = guidanceMessage.trim().lowercase(Locale.US)

    return when {
        safetyFlags.hasStairs -> RouteDetailStepKind.STAIRS
        safetyFlags.hasCurbGap -> RouteDetailStepKind.CURB_GAP
        safetyFlags.hasCrosswalk -> RouteDetailStepKind.CROSSWALK
        normalizedGuidance.containsAnyRouteGuidanceKeyword("엘리베이터", "elevator", "lift") ->
            RouteDetailStepKind.ELEVATOR
        normalizedGuidance.containsAnyRouteGuidanceKeyword("공사", "construction", "우회", "narrow path") ->
            RouteDetailStepKind.CONSTRUCTION
        guidanceDirection == RouteGuidanceDirection.TURN_LEFT -> RouteDetailStepKind.TURN_LEFT
        guidanceDirection == RouteGuidanceDirection.TURN_RIGHT -> RouteDetailStepKind.TURN_RIGHT
        normalizedGuidance.containsAnyRouteGuidanceKeyword("좌회전", "왼쪽", "turn left", "left turn") ->
            RouteDetailStepKind.TURN_LEFT
        normalizedGuidance.containsAnyRouteGuidanceKeyword("우회전", "오른쪽", "turn right", "right turn") ->
            RouteDetailStepKind.TURN_RIGHT
        safetyFlags.hasBrailleBlock -> RouteDetailStepKind.TACTILE_GUIDE
        else -> RouteDetailStepKind.STRAIGHT
    }
}

internal fun RouteSegment.resolveSourceLeg(legs: List<RouteLeg>): RouteLeg? =
    sourceLegSequence?.let { sourceLegSequence ->
        legs.firstOrNull { leg -> leg.sequence == sourceLegSequence }
    }

private fun String.containsAnyRouteGuidanceKeyword(vararg keywords: String): Boolean =
    keywords.any { keyword -> contains(keyword, ignoreCase = true) }
