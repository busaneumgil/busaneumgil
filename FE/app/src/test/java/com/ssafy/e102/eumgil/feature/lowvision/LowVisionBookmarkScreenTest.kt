package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LowVisionBookmarkScreenTest {
    @Test
    fun `bookmark cards follow low vision search result card sizing`() {
        assertEquals(LowVisionSearchLayoutDefaults.resultCardMinHeight, LowVisionBookmarkLayoutDefaults.placeCardMinHeight)
        assertEquals(LowVisionSearchLayoutDefaults.resultCardGap, LowVisionBookmarkLayoutDefaults.placeCardGap)
        assertEquals(LowVisionSearchLayoutDefaults.cardHorizontalPadding, LowVisionBookmarkLayoutDefaults.cardHorizontalPadding)
        assertEquals(LowVisionSearchLayoutDefaults.cardVerticalPadding, LowVisionBookmarkLayoutDefaults.cardVerticalPadding)
    }

    @Test
    fun `bookmark card actions stack like search result actions`() {
        val minimumButtonStackHeight =
            LowVisionBookmarkLayoutDefaults.actionButtonHeight * LowVisionBookmarkLayoutDefaults.actionButtonCount +
                LowVisionBookmarkLayoutDefaults.actionButtonGap

        assertEquals(2, LowVisionBookmarkLayoutDefaults.actionButtonCount)
        assertEquals(LowVisionSearchLayoutDefaults.actionButtonHeight, LowVisionBookmarkLayoutDefaults.actionButtonHeight)
        assertEquals(LowVisionSearchLayoutDefaults.actionButtonGap, LowVisionBookmarkLayoutDefaults.actionButtonGap)
        assertTrue(LowVisionBookmarkLayoutDefaults.placeCardMinHeight >= minimumButtonStackHeight + 160.dp)
        assertEquals(116.dp, LowVisionBookmarkLayoutDefaults.infoSectionMinHeight)
    }

    @Test
    fun `bookmark card text and index sizes match search results`() {
        assertEquals(LowVisionSearchLayoutDefaults.indexBadgeSize, LowVisionBookmarkLayoutDefaults.indexBadgeSize)
        assertEquals(LowVisionSearchLayoutDefaults.titleFontSize, LowVisionBookmarkLayoutDefaults.titleFontSize)
        assertEquals(LowVisionSearchLayoutDefaults.titleLineHeight, LowVisionBookmarkLayoutDefaults.titleLineHeight)
        assertEquals(20.sp, LowVisionBookmarkLayoutDefaults.addressFontSize)
        assertEquals(24.sp, LowVisionBookmarkLayoutDefaults.addressLineHeight)
        assertEquals(LowVisionSearchLayoutDefaults.infoSectionMinHeight, LowVisionBookmarkLayoutDefaults.infoSectionMinHeight)
        assertEquals(LowVisionSearchLayoutDefaults.sectionDividerThickness, LowVisionBookmarkLayoutDefaults.sectionDividerThickness)
    }
}
