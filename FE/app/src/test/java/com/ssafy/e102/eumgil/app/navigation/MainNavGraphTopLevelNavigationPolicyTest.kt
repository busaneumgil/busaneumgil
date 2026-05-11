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
}
