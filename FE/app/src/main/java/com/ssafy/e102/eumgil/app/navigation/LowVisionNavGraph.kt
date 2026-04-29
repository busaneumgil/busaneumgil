package com.ssafy.e102.eumgil.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionHomeRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionVoiceInputRoute
import com.ssafy.e102.eumgil.feature.search.SearchResultsRoute

/**
 * 시각지원 모드 풀스크린 셸의 네비게이션 그래프.
 *
 * Figma file MREqSzkmwhRcXnFS3lzW17, nodes 371:105 (홈) / 371:300 (입력중) 기반.
 * 약관 walkthrough가 완료되면 [LowVisionRoute.Home]으로 진입하도록 호출처에서 연결한다.
 */
fun NavGraphBuilder.lowVisionNavGraph(navController: NavHostController) {
    composable(route = LowVisionRoute.Home.route) {
        LowVisionHomeRoute(
            onVoiceInputClick = {
                navController.navigate(LowVisionRoute.VoiceInput.route)
            },
            onCurrentLocationClick = {
                // 현재 위치 카드는 일반 모드 지도(Map)와 의미가 동일하므로 같은 라우트로 이동.
                navController.navigate(TopLevelRoute.Map.route)
            },
            onTabSelected = { /* 탭 라우팅은 추후 시각지원 모드 전용 4탭 셸이 잡히면 처리. */ },
        )
    }

    composable(route = LowVisionRoute.VoiceInput.route) {
        LowVisionVoiceInputRoute(
            onRecordingFinished = {
                navController.navigate(resolveLowVisionRecordingCompletedRoute()) {
                    launchSingleTop = true
                    popUpTo(resolveLowVisionRecordingPopUpRoute()) {
                        inclusive = true
                    }
                }
            },
            onTabSelected = { /* 탭 라우팅은 추후 시각지원 모드 전용 4탭 셸이 잡히면 처리. */ },
        )
    }

    composable(route = LowVisionRoute.Search.route) {
        SearchResultsRoute(
            initialQuery = "",
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToResults = { _ -> },
            onNavigateToRouteSetting = {
                navController.navigate(resolveLowVisionSearchResultRoute()) {
                    launchSingleTop = true
                    popUpTo(resolveLowVisionSearchPopUpRoute()) {
                        inclusive = true
                    }
                }
            },
        )
    }
}

internal fun resolveLowVisionRecordingCompletedRoute(): String = LowVisionRoute.Search.route

internal fun resolveLowVisionRecordingPopUpRoute(): String = LowVisionRoute.VoiceInput.route

internal fun resolveLowVisionSearchResultRoute(): String =
    RouteSettingRoute.Setting.createRoute(autoStartNavigation = true)

internal fun resolveLowVisionSearchPopUpRoute(): String = LowVisionRoute.Search.route
