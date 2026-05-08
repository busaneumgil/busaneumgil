package com.ssafy.e102.eumgil.app.navigation

import com.ssafy.e102.eumgil.core.model.RouteOption
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
    fun `search and arrival routes keep map tab active`() {
        assertEquals(TopLevelRoute.Map.route, SearchRoute.Entry.route.toCurrentTopLevelRoute())
        assertEquals(TopLevelRoute.Map.route, SearchRoute.Results.route.toCurrentTopLevelRoute())
        assertEquals(TopLevelRoute.Map.route, ArrivalRoute.Entry.route.toCurrentTopLevelRoute())
    }

    @Test
    fun `guidance and route setting routes hide top level tab`() {
        assertNull(NavigationRoute.Guidance.route.toCurrentTopLevelRoute())
        assertNull(RouteSettingRoute.Setting.route.toCurrentTopLevelRoute())
        assertNull(RouteSettingRoute.Detail.createRoute(RouteOption.SAFE).toCurrentTopLevelRoute())
    }

    @Test
    fun `auth onboarding and low vision routes hide top level tab`() {
        assertNull(AuthRoute.Login.route.toCurrentTopLevelRoute())
        assertNull(OnboardingRoute.UserTypePrimary.route.toCurrentTopLevelRoute())
        assertNull(LowVisionRoute.Home.route.toCurrentTopLevelRoute())
        assertNull(LowVisionRoute.Search.route.toCurrentTopLevelRoute())
    }
}
