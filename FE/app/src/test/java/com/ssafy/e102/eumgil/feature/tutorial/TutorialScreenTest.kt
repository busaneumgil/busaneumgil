package com.ssafy.e102.eumgil.feature.tutorial

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TutorialScreenTest {
    @Test
    fun `tutorial uses a frame-free explanation panel instead of a phone mockup`() {
        assertEquals(360.dp, TutorialLayoutDefaults.visualPanelMaxWidth)
        assertEquals(320.dp, TutorialLayoutDefaults.visualPanelMinHeight)
    }

    @Test
    fun `tutorial keeps a consistent three point explanation structure`() {
        assertEquals(3, TutorialLayoutDefaults.totalStepCount)
        assertEquals(3, TutorialLayoutDefaults.supportingItemCount)
    }
}
