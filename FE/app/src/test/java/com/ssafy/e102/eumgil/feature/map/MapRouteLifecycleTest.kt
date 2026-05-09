package com.ssafy.e102.eumgil.feature.map

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapRouteLifecycleTest {
    @Test
    fun `map route starts immediately when lifecycle is already resumed`() {
        assertTrue(shouldStartMapRouteImmediately(Lifecycle.State.RESUMED))
        assertTrue(shouldStartMapRouteImmediately(Lifecycle.State.STARTED))
    }

    @Test
    fun `map route waits for on start when lifecycle is not started yet`() {
        assertFalse(shouldStartMapRouteImmediately(Lifecycle.State.CREATED))
        assertFalse(shouldStartMapRouteImmediately(Lifecycle.State.INITIALIZED))
    }
}
