package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LowVisionHomeScreenTest {
    @Test
    fun `home screen uses two equally sized primary action cards`() {
        assertEquals(252.dp, LowVisionHomeLayoutDefaults.primaryActionCardHeight)
        assertEquals(
            LowVisionHomeLayoutDefaults.primaryActionCardHeight,
            LowVisionHomeLayoutDefaults.currentLocationCardHeight,
        )
    }

    @Test
    fun `home screen hides status guide while voice output is not wired`() {
        assertFalse(LowVisionHomeLayoutDefaults.showsStatusGuide)
    }
}
