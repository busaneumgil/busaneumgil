package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LowVisionSearchScreenTest {
    @Test
    fun `search result cards keep enough height for compact stacked action buttons`() {
        val minimumButtonStackHeight =
            LowVisionSearchLayoutDefaults.actionButtonHeight * LowVisionSearchLayoutDefaults.actionButtonCount +
                LowVisionSearchLayoutDefaults.actionButtonGap

        assertEquals(2, LowVisionSearchLayoutDefaults.actionButtonCount)
        assertEquals(58.dp, LowVisionSearchLayoutDefaults.actionButtonHeight)
        assertEquals(10.dp, LowVisionSearchLayoutDefaults.actionButtonGap)
        assertTrue(LowVisionSearchLayoutDefaults.resultCardMinHeight >= minimumButtonStackHeight + 170.dp)
    }

    @Test
    fun `search result list can show two compact result cards`() {
        val twoCardHeight =
            LowVisionSearchLayoutDefaults.resultCardMinHeight * 2 +
                LowVisionSearchLayoutDefaults.resultCardGap

        assertEquals(320.dp, LowVisionSearchLayoutDefaults.resultCardMinHeight)
        assertEquals(12.dp, LowVisionSearchLayoutDefaults.resultCardGap)
        assertTrue(twoCardHeight <= LowVisionSearchLayoutDefaults.twoCardViewportBudget)
    }

    @Test
    fun `search result card text scale is reduced for two result layout`() {
        assertEquals(34.sp, LowVisionSearchLayoutDefaults.titleLineHeight)
        assertEquals(24.sp, LowVisionSearchLayoutDefaults.addressLineHeight)
        assertEquals(64.dp, LowVisionSearchLayoutDefaults.resultListBottomPadding)
    }
}
