package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.core.model.RouteDefaults.DEFAULT_GUIDANCE_MESSAGE
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.feature.route.RouteDetailStepKind
import com.ssafy.e102.eumgil.feature.route.toRouteDetailStepKind
import java.util.Locale

internal data class NavigationHeroDetailUiState(
    val title: String,
    val description: String,
    val guidanceAction: NavigationGuidanceAction,
)

internal fun RouteSegment.toNavigationHeroDetail(): NavigationHeroDetailUiState {
    val kind = toRouteDetailStepKind()

    return NavigationHeroDetailUiState(
        title = navigationHeroDetailTitle(kind),
        description = navigationHeroDetailDescription(kind),
        guidanceAction = toNavigationGuidanceAction(),
    )
}

private fun RouteSegment.navigationHeroDetailTitle(kind: RouteDetailStepKind): String =
    when (kind) {
        RouteDetailStepKind.START -> "출발"
        RouteDetailStepKind.STRAIGHT -> "직진 이동"
        RouteDetailStepKind.TURN_LEFT -> "좌회전"
        RouteDetailStepKind.TURN_RIGHT -> "우회전"
        RouteDetailStepKind.TACTILE_GUIDE -> "점자블록 따라 이동"
        RouteDetailStepKind.CROSSWALK -> "횡단보도 건너기"
        RouteDetailStepKind.ELEVATOR -> "엘리베이터 이용"
        RouteDetailStepKind.CONSTRUCTION -> "공사 구간 진입"
        RouteDetailStepKind.CURB_GAP -> "단차 구간 주의"
        RouteDetailStepKind.STAIRS -> "계단 구간 주의"
        RouteDetailStepKind.ARRIVAL -> "도착"
        RouteDetailStepKind.FALLBACK -> "세부 경로 확인 중"
    }

private fun RouteSegment.navigationHeroDetailDescription(kind: RouteDetailStepKind): String {
    val distanceLabel = distanceMeters.toNavigationHeroDistanceLabel()
    val guidanceFallback = guidanceMessage.takeIf(String::hasVisibleHangul)

    if (guidanceFallback != null && guidanceFallback != DEFAULT_GUIDANCE_MESSAGE && kind != RouteDetailStepKind.CROSSWALK) {
        return guidanceFallback
    }

    return when (kind) {
        RouteDetailStepKind.START -> "현재 위치에서 선택한 경로 안내를 시작합니다."
        RouteDetailStepKind.STRAIGHT ->
            if (distanceMeters > 0) {
                "$distanceLabel 정도 직진으로 이동하세요."
            } else {
                HERO_DETAIL_GENERIC_DESCRIPTION
            }

        RouteDetailStepKind.TURN_LEFT ->
            if (distanceMeters > 0) {
                "$distanceLabel 정도 이동 후 왼쪽 방향으로 이동하세요."
            } else {
                "왼쪽 방향으로 이동하세요."
            }

        RouteDetailStepKind.TURN_RIGHT ->
            if (distanceMeters > 0) {
                "$distanceLabel 정도 이동 후 오른쪽 방향으로 이동하세요."
            } else {
                "오른쪽 방향으로 이동하세요."
            }

        RouteDetailStepKind.TACTILE_GUIDE -> "점자블록 유도를 따라 주변 보행 흐름을 유지하고 이동하세요."
        RouteDetailStepKind.CROSSWALK ->
            when {
                safetyFlags.hasAudioSignal -> "음향 신호기 안내를 확인한 뒤 횡단보도를 건너세요."
                safetyFlags.hasSignal -> "신호를 확인한 뒤 횡단보도를 건너세요."
                else -> "주변 차량이 멈췄는지 확인한 뒤 횡단보도를 조심해서 건너세요."
            }

        RouteDetailStepKind.ELEVATOR -> "안내된 엘리베이터를 이용해 다음 구간으로 이동하세요."
        RouteDetailStepKind.CONSTRUCTION -> "공사로 통로가 좁을 수 있어 주변을 확인하며 지나가세요."
        RouteDetailStepKind.CURB_GAP -> "연석 단차가 있어 속도를 줄이고 바퀴 각도를 맞춰 이동하세요."
        RouteDetailStepKind.STAIRS -> "계단이 포함된 구간이어서 보조가 필요할 수 있습니다."
        RouteDetailStepKind.ARRIVAL -> HERO_DETAIL_GENERIC_DESCRIPTION
        RouteDetailStepKind.FALLBACK -> HERO_DETAIL_FALLBACK_DESCRIPTION
    }
}

private fun String.hasVisibleHangul(): Boolean = any { character -> character in '\uAC00'..'\uD7A3' }

private fun Int.toNavigationHeroDistanceLabel(): String =
    when {
        this <= 0 -> HERO_DETAIL_PENDING_VALUE
        this < HERO_DETAIL_METERS_PER_KILOMETER -> "$this m"
        else -> String.format(Locale.US, "%.1f km", this / HERO_DETAIL_METERS_PER_KILOMETER.toFloat())
    }

private const val HERO_DETAIL_METERS_PER_KILOMETER = 1_000
private const val HERO_DETAIL_PENDING_VALUE = "정보 준비 중"
private const val HERO_DETAIL_GENERIC_DESCRIPTION = "이 구간의 세부 정보는 준비 중입니다."
private const val HERO_DETAIL_FALLBACK_DESCRIPTION = "세부 이동 정보는 준비 중입니다. 요약 정보와 주의 구간을 먼저 확인하세요."
