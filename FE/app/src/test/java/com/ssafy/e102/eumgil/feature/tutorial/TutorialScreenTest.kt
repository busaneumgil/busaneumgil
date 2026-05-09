package com.ssafy.e102.eumgil.feature.tutorial

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialScreenTest {
    @Test
    fun `tutorial keeps overlay highlight prominent without using live screen overlay`() {
        assertEquals(4.dp, TutorialLayoutDefaults.highlightStrokeWidth)
        assertTrue(TutorialLayoutDefaults.phoneMockupMaxHeight >= 420.dp)
    }

    @Test
    fun `tutorial reserves stable mock viewport dimensions`() {
        assertEquals(328.dp, TutorialLayoutDefaults.phoneMockupWidth)
        assertEquals(456.dp, TutorialLayoutDefaults.phoneMockupMaxHeight)
        assertEquals(3, TutorialLayoutDefaults.totalStepCount)
    }
}
