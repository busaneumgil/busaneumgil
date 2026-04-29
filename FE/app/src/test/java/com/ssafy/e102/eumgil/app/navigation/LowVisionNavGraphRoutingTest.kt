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
    fun `low vision uses dedicated navigation ui only for low vision user type`() {
        assertEquals(true, shouldUseLowVisionNavigationUi(selectedPrimaryUserType = "low_vision"))
        assertEquals(false, shouldUseLowVisionNavigationUi(selectedPrimaryUserType = "mobility_impaired"))
        assertEquals(false, shouldUseLowVisionNavigationUi(selectedPrimaryUserType = null))
    }

    @Test
    fun `low vision bottom tabs resolve to app destinations`() {
        assertEquals(LowVisionRoute.Home.route, resolveLowVisionBottomTabRoute(LowVisionBottomTab.HOME))
        assertEquals(LowVisionRoute.Bookmark.route, resolveLowVisionBottomTabRoute(LowVisionBottomTab.BOOKMARK))
        assertEquals(LowVisionRoute.Search.route, resolveLowVisionBottomTabRoute(LowVisionBottomTab.CATEGORY))
        assertEquals(LowVisionRoute.MyPage.route, resolveLowVisionBottomTabRoute(LowVisionBottomTab.MY_PAGE))
    }

    @Test
    fun `low vision my page actions resolve to concrete destinations`() {
        assertEquals(
            OnboardingRoute.ProfileUserTypePrimary.route,
            resolveLowVisionModeChangeRoute(),
        )
        assertEquals(
            LowVisionRoute.AppInfo.route,
            resolveLowVisionAppInfoRoute(),
        )
        assertEquals(
            AuthRoute.Login.route,
            resolveLowVisionLogoutRoute(),
        )
    }
}
