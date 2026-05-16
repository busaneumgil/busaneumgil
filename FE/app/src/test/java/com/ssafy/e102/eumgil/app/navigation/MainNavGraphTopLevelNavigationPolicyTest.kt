package com.ssafy.e102.eumgil.app.navigation

import androidx.lifecycle.SavedStateHandle
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainNavGraphTopLevelNavigationPolicyTest {
    @Test
    fun `bottom tab selection preserves each tab state across reentry`() {
        assertTrue(DefaultTopLevelNavigationPolicy.launchSingleTop)
        assertTrue(DefaultTopLevelNavigationPolicy.restoreState)
        assertTrue(DefaultTopLevelNavigationPolicy.saveState)
    }

    @Test
    fun `top level navigation policy remains stable`() {
        assertEquals(
            TopLevelNavigationPolicy(
                launchSingleTop = true,
                restoreState = true,
                saveState = true,
            ),
            DefaultTopLevelNavigationPolicy,
        )
    }

    @Test
    fun `map home reentry signal is consumed once`() {
        val savedStateHandle = SavedStateHandle()

        savedStateHandle.requestMapHomeReentryReset()

        assertTrue(savedStateHandle.consumeMapHomeReentryReset())
        assertFalse(savedStateHandle.consumeMapHomeReentryReset())
    }

    @Test
    fun `map home reentry helper pops the existing map entry before fallback navigate`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/app/navigation/MainNavGraph.kt")
                .readText()

        assertTrue(
            "Home reentry should first try to pop back to the existing map entry so alias routes like search return to the actual map screen immediately.",
            source.contains("popBackStack(") &&
                source.contains("route = TopLevelRoute.Map.route") &&
                source.contains("inclusive = false"),
        )
        assertTrue(
            "Home reentry should still fall back to top-level map navigation when no existing map entry is available.",
            source.contains("if (!didPopToMap) {") &&
                source.contains("navigateToTopLevel(TopLevelDestination.Map)"),
        )
    }

    @Test
    fun `route setting close actions navigate through map home reentry helper`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/app/navigation/MainNavGraph.kt")
                .readText()
        val entryRouteSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingEntryRoute.kt")
                .readText()
        val routeSettingDestination =
            source
                .substringAfter("route = RouteSettingRoute.Setting.route")
                .substringBefore("route = RouteSettingRoute.Detail.route")
        val routeDetailDestination =
            source
                .substringAfter("route = RouteSettingRoute.Detail.route")
                .substringBefore("composable(route = ReportRoute.Report.route)")
        val routeDetailEntry =
            entryRouteSource
                .substringAfter("fun RouteDetailEntryRoute(")

        assertTrue(
            "Route setting close should return to map home instead of a plain back-stack pop.",
            routeSettingDestination.contains("onNavigateToMap = {") &&
                routeSettingDestination.contains("navController.navigateToTopLevelMapForHomeEntry()"),
        )
        assertTrue(
            "Route detail close should also return to map home.",
            routeDetailDestination.contains("onNavigateToMap = {") &&
                routeDetailDestination.contains("navController.navigateToTopLevelMapForHomeEntry()"),
        )
        assertTrue(
            "Route detail top-bar X should invoke the map-home callback directly instead of falling back through the route flow back stack.",
            routeDetailEntry.contains("onCloseClick = onNavigateToMap"),
        )
    }

    @Test
    fun `map home reentry reset excludes bookmark while keeping other visible non-map routes`() {
        assertTrue(
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = SearchRoute.Entry.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertFalse(
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = TopLevelRoute.SavedRoute.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertTrue(
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = ReportRoute.Report.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertTrue(
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = TopLevelRoute.MyPage.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertFalse(
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = TopLevelRoute.Map.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertFalse(
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = ArrivalRoute.Entry.route,
                destination = TopLevelDestination.Map,
            ),
        )
    }
}
