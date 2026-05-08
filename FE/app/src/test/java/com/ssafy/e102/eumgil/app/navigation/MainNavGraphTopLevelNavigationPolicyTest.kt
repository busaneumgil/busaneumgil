package com.ssafy.e102.eumgil.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainNavGraphTopLevelNavigationPolicyTest {
    @Test
    fun `bottom tab selection resets each tab to its landing screen`() {
        assertTrue(DefaultTopLevelNavigationPolicy.launchSingleTop)
        assertFalse(DefaultTopLevelNavigationPolicy.restoreState)
        assertFalse(DefaultTopLevelNavigationPolicy.saveState)
    }

    @Test
    fun `top level navigation policy remains stable`() {
        assertEquals(
            TopLevelNavigationPolicy(
                launchSingleTop = true,
                restoreState = false,
                saveState = false,
            ),
            DefaultTopLevelNavigationPolicy,
        )
    }
}
