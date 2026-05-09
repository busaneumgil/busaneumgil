package com.ssafy.e102.eumgil.app.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionBottomTab
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionAppInfoRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionBookmarkRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionCategoryRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionEvent
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionHomeRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionMyPageRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionNavigationCompleteRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionNavigationRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionRouteBriefingRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionSearchRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionViewModel
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionVoiceInputRoute
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionBottomNav

/**
 * LowVision 중첩 네비게이션 그래프의 루트 경로.
 *
 * `navigation(route = LOW_VISION_GRAPH_ROUTE, ...)` 블록에 스코프된
 * [LowVisionViewModel]이 이 그래프 안의 모든 탭 화면에서 공유된다.
 */
internal const val LOW_VISION_GRAPH_ROUTE = "low_vision_graph"

fun NavGraphBuilder.lowVisionNavGraph(navController: NavHostController) {
    navigation(
        route = LOW_VISION_GRAPH_ROUTE,
        startDestination = LowVisionRoute.Home.route,
    ) {
        lowVisionComposable(route = LowVisionRoute.Home.route) { backStackEntry ->
            // 공유 ViewModel — 탭 전환 시에도 동일 인스턴스 (KWS 1개만 실행)
            LowVisionKwsNavEffect(
                navController = navController,
                backStackEntry = backStackEntry,
            )

            val context = LocalContext.current
            val micPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                if (isGranted) navController.navigate(LowVisionRoute.VoiceInput.route)
            }

            LowVisionHomeRoute(
                onVoiceInputClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        navController.navigate(LowVisionRoute.VoiceInput.route)
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onCurrentLocationClick = {
                    resolveLowVisionCurrentLocationRoute()?.let(navController::navigate)
                },
                onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
            )
        }

        // VoiceInput — KWS 제외 (STT AudioRecorder가 마이크 점유)
        lowVisionComposable(route = LowVisionRoute.VoiceInput.route) {
            LowVisionVoiceInputRoute(
                onCancelRecording = {
                    navController.navigate(resolveLowVisionVoiceInputCancelRoute()) {
                        launchSingleTop = true
                        popUpTo(resolveLowVisionRecordingPopUpRoute()) {
                            inclusive = true
                        }
                    }
                },
                onRecordingCompleted = { query ->
                    navController.navigate(LowVisionRoute.VoiceSearch.createRoute(query)) {
                        launchSingleTop = true
                        popUpTo(resolveLowVisionRecordingPopUpRoute()) {
                            inclusive = true
                        }
                    }
                },
                onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
            )
        }

        lowVisionComposable(
            route = LowVisionRoute.VoiceSearch.route,
            arguments = listOf(
                navArgument(LowVisionRoute.VoiceSearch.ARG_QUERY) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            LowVisionKwsNavEffect(navController = navController, backStackEntry = backStackEntry)
            val encodedQuery = backStackEntry.arguments?.getString(LowVisionRoute.VoiceSearch.ARG_QUERY).orEmpty()
            val query = URLDecoder.decode(encodedQuery, StandardCharsets.UTF_8.toString()).trim()
            LowVisionSearchResultShell(
                navController = navController,
                selectedTab = LowVisionBottomTab.HOME,
                initialQuery = query,
            )
        }

        lowVisionComposable(route = LowVisionRoute.Bookmark.route) { backStackEntry ->
            LowVisionKwsNavEffect(
                navController = navController,
                backStackEntry = backStackEntry,
            )
            LowVisionBookmarkRoute(
                onNavigateToRouteSetting = {
                    navController.navigate(LowVisionRoute.Guidance.route)
                },
                onNavigateToRouteBriefing = {
                    navController.navigate(LowVisionRoute.RouteBriefing.route) {
                        launchSingleTop = true
                    }
                },
                onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
            )
        }

        lowVisionComposable(route = LowVisionRoute.Search.route) { backStackEntry ->
            LowVisionKwsNavEffect(
                navController = navController,
                backStackEntry = backStackEntry,
            )
            LowVisionSearchResultShell(
                navController = navController,
                selectedTab = LowVisionBottomTab.HOME,
                initialQuery = "",
            )
        }

        lowVisionComposable(route = LowVisionRoute.CategorySearch.route) { backStackEntry ->
            LowVisionKwsNavEffect(
                navController = navController,
                backStackEntry = backStackEntry,
            )
            LowVisionCategoryRoute(
                onCategorySelected = { category ->
                    navController.navigate(LowVisionRoute.CategoryResult.createRoute(category))
                },
                onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
            )
        }

        lowVisionComposable(
            route = LowVisionRoute.CategoryResult.route,
            arguments =
                listOf(
                    navArgument(LowVisionRoute.CategoryResult.ARG_CATEGORY) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            LowVisionKwsNavEffect(
                navController = navController,
                backStackEntry = backStackEntry,
            )
            val category = backStackEntry.arguments?.getString(LowVisionRoute.CategoryResult.ARG_CATEGORY).orEmpty()
            LowVisionSearchResultShell(
                navController = navController,
                selectedTab = LowVisionBottomTab.CATEGORY,
                initialQuery = category,
                categoryLabel = category,
            )
        }

        lowVisionComposable(route = LowVisionRoute.RouteBriefing.route) { backStackEntry ->
            LowVisionKwsNavEffect(
                navController = navController,
                backStackEntry = backStackEntry,
            )
            LowVisionRouteBriefingRoute(
                onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
            )
        }

        // Guidance — KWS 제외 (음성 안내가 실행 중일 수 있음)
        lowVisionComposable(route = LowVisionRoute.Guidance.route) {
            LowVisionNavigationRoute(
                onNavigateToComplete = {
                    navController.navigate(resolveLowVisionNavigationExitRoute()) {
                        launchSingleTop = true
                        popUpTo(LowVisionRoute.Guidance.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToBookmark = {
                    navController.navigate(LowVisionRoute.Bookmark.route) {
                        launchSingleTop = true
                        popUpTo(LowVisionRoute.Guidance.route) {
                            inclusive = true
                        }
                    }
                },
                onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
            )
        }

        lowVisionComposable(route = LowVisionRoute.NavigationComplete.route) { backStackEntry ->
            LowVisionKwsNavEffect(
                navController = navController,
                backStackEntry = backStackEntry,
            )
            LowVisionNavigationCompleteRoute(
                onNavigateToBookmark = {
                    navController.navigate(LowVisionRoute.Bookmark.route) {
                        launchSingleTop = true
                        popUpTo(LowVisionRoute.NavigationComplete.route) {
                            inclusive = true
                        }
                    }
                },
                onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
            )
        }

        lowVisionComposable(route = LowVisionRoute.MyPage.route) { backStackEntry ->
            LowVisionKwsNavEffect(
                navController = navController,
                backStackEntry = backStackEntry,
            )
            LowVisionMyPageRoute(
                onModeChangeClick = {
                    navController.navigate(resolveLowVisionModeChangeRoute())
                },
                onAppInfoClick = {
                    navController.navigate(resolveLowVisionAppInfoRoute())
                },
                onLogoutClick = {
                    navController.navigate(resolveLowVisionLogoutRoute()) {
                        launchSingleTop = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                },
                onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
            )
        }

        lowVisionComposable(route = LowVisionRoute.AppInfo.route) { backStackEntry ->
            LowVisionKwsNavEffect(
                navController = navController,
                backStackEntry = backStackEntry,
            )
            LowVisionAppInfoRoute(
                onNavigateToLogin = {
                    navController.navigate(resolveLowVisionLogoutRoute()) {
                        launchSingleTop = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                },
                onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
            )
        }
    }
}

/**
 * LowVision 그래프 공유 [LowVisionViewModel]을 통해 웨이크워드 감지 시
 * VoiceInput 화면으로 이동하는 side-effect composable.
 *
 * [LowVisionViewModel]은 [LOW_VISION_GRAPH_ROUTE] NavBackStackEntry에 스코프되어
 * 모든 탭에서 동일 인스턴스를 공유한다. KWS AudioRecorder는 오직 1개만 실행된다.
 *
 * VoiceInput·Guidance 화면에서는 호출하지 않는다 (마이크 충돌 방지).
 */
@Composable
private fun LowVisionKwsNavEffect(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
) {
    // 중첩 그래프 back stack entry에 스코프된 공유 ViewModel
    val graphEntry = remember(backStackEntry) {
        navController.getBackStackEntry(LOW_VISION_GRAPH_ROUTE)
    }
    val viewModel: LowVisionViewModel = viewModel(graphEntry)

    LaunchedEffect(viewModel) {
        // 탭 화면 진입 시마다 KWS 재시작 (VoiceInput 사용 후 복귀 포함)
        viewModel.resumeSpotting()
        viewModel.uiEvent.collect { event ->
            when (event) {
                LowVisionEvent.NavigateToVoiceInput -> {
                    // KWS가 동작 중이었으므로 RECORD_AUDIO 권한은 이미 허가된 상태
                    navController.navigate(LowVisionRoute.VoiceInput.route) {
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}

@Composable
private fun LowVisionSearchResultShell(
    navController: NavHostController,
    selectedTab: LowVisionBottomTab,
    initialQuery: String,
    categoryLabel: String? = null,
) {
    val searchPopUpRoute = resolveLowVisionSearchPopUpRoute(selectedTab)
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        LowVisionSearchRoute(
            initialQuery = initialQuery,
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToRouteSetting = {
                navController.navigate(LowVisionRoute.Guidance.route) {
                    launchSingleTop = true
                    popUpTo(searchPopUpRoute) {
                        inclusive = true
                    }
                }
            },
            onNavigateToRouteBriefing = {
                navController.navigate(LowVisionRoute.RouteBriefing.route) {
                    launchSingleTop = true
                }
            },
            onNavigateToBookmark = {
                navController.navigate(LowVisionRoute.Bookmark.route) {
                    launchSingleTop = true
                    popUpTo(searchPopUpRoute) {
                        inclusive = true
                    }
                }
            },
            modifier = Modifier.weight(1f),
            categoryLabel = categoryLabel,
        )

        LowVisionBottomNav(
            selectedTab = selectedTab,
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
    }
}

internal fun resolveLowVisionRecordingCompletedRoute(): String = LowVisionRoute.Search.route

internal fun resolveLowVisionVoiceInputCancelRoute(): String = LowVisionRoute.Home.route

internal fun resolveLowVisionRecordingPopUpRoute(): String = LowVisionRoute.VoiceInput.route

internal fun resolveLowVisionSearchResultRoute(): String =
    LowVisionRoute.Guidance.route

internal fun resolveLowVisionSearchPopUpRoute(selectedTab: LowVisionBottomTab = LowVisionBottomTab.HOME): String =
    when (selectedTab) {
        LowVisionBottomTab.CATEGORY -> LowVisionRoute.CategorySearch.route
        else -> LowVisionRoute.Search.route
    }

internal fun resolveNavigationCompletionRoute(): String = ArrivalRoute.Entry.route

internal fun resolveLowVisionNavigationExitRoute(): String = LowVisionRoute.Home.route

internal fun resolveLowVisionCurrentLocationRoute(): String? = null

internal fun resolveLowVisionModeChangeRoute(): String = OnboardingRoute.ProfileUserTypePrimary.route

internal fun resolveLowVisionAppInfoRoute(): String = LowVisionRoute.AppInfo.route

internal fun resolveLowVisionLogoutRoute(): String = AuthRoute.Login.route

internal fun resolveLowVisionBottomTabRoute(tab: LowVisionBottomTab): String =
    when (tab) {
        LowVisionBottomTab.HOME -> LowVisionRoute.Home.route
        LowVisionBottomTab.BOOKMARK -> LowVisionRoute.Bookmark.route
        LowVisionBottomTab.CATEGORY -> LowVisionRoute.CategorySearch.route
        LowVisionBottomTab.MY_PAGE -> LowVisionRoute.MyPage.route
    }

internal fun resolveLowVisionSelectedBottomTab(currentRoute: String?): LowVisionBottomTab? =
    when (currentRoute) {
        LowVisionRoute.Home.route,
        LowVisionRoute.VoiceInput.route,
        LowVisionRoute.Search.route,
        LowVisionRoute.VoiceSearch.route,
        LowVisionRoute.RouteBriefing.route,
        LowVisionRoute.Guidance.route,
        LowVisionRoute.NavigationComplete.route -> LowVisionBottomTab.HOME
        LowVisionRoute.Bookmark.route -> LowVisionBottomTab.BOOKMARK
        LowVisionRoute.CategorySearch.route,
        LowVisionRoute.CategoryResult.route -> LowVisionBottomTab.CATEGORY
        LowVisionRoute.MyPage.route,
        LowVisionRoute.AppInfo.route -> LowVisionBottomTab.MY_PAGE
        else -> null
    }

internal fun shouldNavigateLowVisionBottomTab(
    currentRoute: String?,
    selectedTab: LowVisionBottomTab,
): Boolean = resolveLowVisionSelectedBottomTab(currentRoute) != selectedTab

internal fun shouldUseInstantLowVisionDestinationTransitions(): Boolean = true

private fun NavGraphBuilder.lowVisionComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = { lowVisionEnterTransition() },
        exitTransition = { lowVisionExitTransition() },
        popEnterTransition = { lowVisionEnterTransition() },
        popExitTransition = { lowVisionExitTransition() },
    ) { backStackEntry ->
        content(backStackEntry)
    }
}

private fun lowVisionEnterTransition(): EnterTransition? =
    if (shouldUseInstantLowVisionDestinationTransitions()) {
        EnterTransition.None
    } else {
        null
    }

private fun lowVisionExitTransition(): ExitTransition? =
    if (shouldUseInstantLowVisionDestinationTransitions()) {
        ExitTransition.None
    } else {
        null
    }

private fun NavHostController.navigateToLowVisionBottomTab(tab: LowVisionBottomTab) {
    if (!shouldNavigateLowVisionBottomTab(currentBackStackEntry?.destination?.route, tab)) {
        return
    }

    when (tab) {
        LowVisionBottomTab.HOME -> {
            val didPopHome =
                popBackStack(
                    route = LowVisionRoute.Home.route,
                    inclusive = false,
                )
            if (!didPopHome) {
                navigate(LowVisionRoute.Home.route) {
                    launchSingleTop = true
                }
            }
        }
        LowVisionBottomTab.BOOKMARK -> {
            navigate(LowVisionRoute.Bookmark.route) {
                launchSingleTop = true
                popUpTo(LowVisionRoute.Home.route) {
                    inclusive = false
                }
            }
        }
        LowVisionBottomTab.CATEGORY -> {
            navigate(LowVisionRoute.CategorySearch.route) {
                launchSingleTop = true
                popUpTo(LowVisionRoute.Home.route) {
                    inclusive = false
                }
            }
        }
        LowVisionBottomTab.MY_PAGE -> {
            navigate(LowVisionRoute.MyPage.route) {
                launchSingleTop = true
                popUpTo(LowVisionRoute.Home.route) {
                    inclusive = false
                }
            }
        }
    }
}
