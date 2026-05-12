package com.ssafy.e102.eumgil.app.navigation

import androidx.lifecycle.SavedStateHandle
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
