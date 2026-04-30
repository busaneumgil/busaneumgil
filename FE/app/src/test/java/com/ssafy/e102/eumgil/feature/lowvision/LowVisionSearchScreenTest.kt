package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LowVisionSearchScreenTest {
    @Test
    fun `search result cards keep enough height for stacked action buttons`() {
        val minimumButtonStackHeight =
            LowVisionSearchLayoutDefaults.actionButtonHeight * LowVisionSearchLayoutDefaults.actionButtonCount +
                LowVisionSearchLayoutDefaults.actionButtonGap

        assertEquals(2, LowVisionSearchLayoutDefaults.actionButtonCount)
        assertEquals(78.dp, LowVisionSearchLayoutDefaults.actionButtonHeight)
        assertEquals(16.dp, LowVisionSearchLayoutDefaults.actionButtonGap)
        assertTrue(LowVisionSearchLayoutDefaults.resultCardMinHeight >= minimumButtonStackHeight + 300.dp)
    }

    @Test
    fun `search result list keeps scroll padding below tall result cards`() {
        assertEquals(560.dp, LowVisionSearchLayoutDefaults.resultCardMinHeight)
        assertEquals(96.dp, LowVisionSearchLayoutDefaults.resultListBottomPadding)
    }
}
