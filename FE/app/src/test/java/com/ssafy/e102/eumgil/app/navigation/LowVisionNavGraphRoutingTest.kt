package com.ssafy.e102.eumgil.app.navigation

import com.ssafy.e102.eumgil.feature.lowvision.LowVisionBottomTab
import org.junit.Assert.assertEquals
import org.junit.Test

class LowVisionNavGraphRoutingTest {
    @Test
    fun `recording completion moves to low vision search results`() {
        assertEquals(
            LowVisionRoute.Search.route,
            resolveLowVisionRecordingCompletedRoute(),
        )
        assertEquals(
            LowVisionRoute.VoiceInput.route,
            resolveLowVisionRecordingPopUpRoute(),
        )
    }

    @Test
    fun `low vision search result auto starts navigation guidance`() {
        assertEquals(
            RouteSettingRoute.Setting.createRoute(autoStartNavigation = true),
            resolveLowVisionSearchResultRoute(),
        )
        assertEquals(
            LowVisionRoute.Search.route,
            resolveLowVisionSearchPopUpRoute(),
        )
    }

    @Test
    fun `navigation completion returns to low vision voice input home tab`() {
        assertEquals(
            LowVisionRoute.Home.route,
            resolveNavigationCompletionRoute(),
        )
    }

    @Test
    fun `low vision bottom tabs resolve to app destinations`() {
        assertEquals(LowVisionRoute.Home.route, resolveLowVisionBottomTabRoute(LowVisionBottomTab.HOME))
        assertEquals(TopLevelRoute.SavedRoute.route, resolveLowVisionBottomTabRoute(LowVisionBottomTab.BOOKMARK))
        assertEquals(LowVisionRoute.Search.route, resolveLowVisionBottomTabRoute(LowVisionBottomTab.CATEGORY))
        assertEquals(TopLevelRoute.MyPage.route, resolveLowVisionBottomTabRoute(LowVisionBottomTab.MY_PAGE))
    }
}
