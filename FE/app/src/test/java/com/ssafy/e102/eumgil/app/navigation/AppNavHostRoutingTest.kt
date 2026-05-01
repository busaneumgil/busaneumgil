package com.ssafy.e102.eumgil.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppNavHostRoutingTest {
    @Test
    fun `top level destination entries expose concrete routes`() {
        assertEquals(
            listOf(
                TopLevelRoute.Map.route,
                TopLevelRoute.SavedRoute.route,
                ReportRoute.Report.route,
                TopLevelRoute.MyPage.route,
            ),
            TopLevelDestination.entries.map { destination -> destination.route.route },
        )
    }

    @Test
    fun `guidance and handoff routes keep map tab active`() {
        assertEquals(TopLevelRoute.Map.route, NavigationRoute.Guidance.route.toCurrentTopLevelRoute())
        assertEquals(TopLevelRoute.Map.route, SearchRoute.Entry.route.toCurrentTopLevelRoute())
        assertEquals(TopLevelRoute.Map.route, SearchRoute.Results.route.toCurrentTopLevelRoute())
        assertEquals(TopLevelRoute.Map.route, RouteSettingRoute.Setting.route.toCurrentTopLevelRoute())
        assertEquals(TopLevelRoute.Map.route, "arrival".toCurrentTopLevelRoute())
    }

    @Test
    fun `auth onboarding and low vision routes hide top level tab`() {
        assertNull(AuthRoute.Login.route.toCurrentTopLevelRoute())
        assertNull(OnboardingRoute.UserTypePrimary.route.toCurrentTopLevelRoute())
        assertNull(LowVisionRoute.Home.route.toCurrentTopLevelRoute())
        assertNull(LowVisionRoute.Search.route.toCurrentTopLevelRoute())
    }
}
