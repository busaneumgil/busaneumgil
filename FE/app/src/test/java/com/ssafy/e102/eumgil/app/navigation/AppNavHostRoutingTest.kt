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
    fun `search routes keep map tab active while arrival hides top level tab`() {
        assertEquals(TopLevelRoute.Map.route, SearchRoute.Entry.route.toCurrentTopLevelRoute())
        assertEquals(TopLevelRoute.Map.route, SearchRoute.Results.route.toCurrentTopLevelRoute())
        assertNull(ArrivalRoute.Entry.route.toCurrentTopLevelRoute())
    }

    @Test
    fun `map tab skips home reentry reset for bookmark while keeping other visible non-map routes`() {
        assertEquals(
            true,
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = SearchRoute.Entry.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertEquals(
            true,
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = SearchRoute.Results.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertEquals(
            false,
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = ArrivalRoute.Entry.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertEquals(
            false,
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = TopLevelRoute.Map.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertEquals(
            false,
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = TopLevelRoute.SavedRoute.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertEquals(
            true,
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = ReportRoute.Report.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertEquals(
            true,
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = TopLevelRoute.MyPage.route,
                destination = TopLevelDestination.Map,
            ),
        )
        assertEquals(
            false,
            shouldNavigateToTopLevelMapForHomeEntry(
                currentRoute = SearchRoute.Entry.route,
                destination = TopLevelDestination.SavedRoute,
            ),
        )
    }

    @Test
    fun `guidance and route setting routes hide top level tab`() {
        assertNull(NavigationRoute.Guidance.route.toCurrentTopLevelRoute())
        assertNull(RouteSettingRoute.Setting.route.toCurrentTopLevelRoute())
        assertNull(RouteSettingRoute.Detail.createRoute(RouteOption.SAFE).toCurrentTopLevelRoute())
    }

    @Test
    fun `search result name tap resolves to route briefing`() {
        assertEquals(LowVisionRoute.RouteBriefing.route, resolveSearchResultBriefingRoute())
    }

    @Test
    fun `arrival home return matches the active user type`() {
        assertEquals(
            LowVisionRoute.Home.route,
            resolveArrivalHomeRoute(selectedPrimaryUserType = "low_vision"),
        )
        assertEquals(
            TopLevelRoute.Map.route,
            resolveArrivalHomeRoute(selectedPrimaryUserType = "mobility_impaired"),
        )
        assertEquals(
            TopLevelRoute.Map.route,
            resolveArrivalHomeRoute(selectedPrimaryUserType = null),
        )
    }

    @Test
    fun `auth onboarding and low vision routes hide top level tab`() {
        assertNull(AuthRoute.Login.route.toCurrentTopLevelRoute())
        assertNull(OnboardingRoute.UserTypePrimary.route.toCurrentTopLevelRoute())
        assertNull(LowVisionRoute.Home.route.toCurrentTopLevelRoute())
        assertNull(LowVisionRoute.Search.route.toCurrentTopLevelRoute())
    }

    @Test
    fun `app route changes use instant destination transitions`() {
        assertEquals(true, shouldUseInstantAppDestinationTransitions())
    }
}
