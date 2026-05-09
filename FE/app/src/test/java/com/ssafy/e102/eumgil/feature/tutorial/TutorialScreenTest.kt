package com.ssafy.e102.eumgil.feature.tutorial

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TutorialScreenTest {
    @Test
    fun `tutorial uses a frame-free explanation panel that fills the body area`() {
        assertEquals(360.dp, TutorialLayoutDefaults.visualPanelMaxWidth)
        assertEquals(1f, TutorialLayoutDefaults.visualPanelWeight, 0f)
    }

    @Test
    fun `tutorial keeps a consistent three point explanation structure`() {
        assertEquals(3, TutorialLayoutDefaults.totalStepCount)
        assertEquals(3, TutorialLayoutDefaults.supportingItemCount)
        assertEquals(3, TutorialLayoutDefaults.destinationFilterChipCount)
    }
}
