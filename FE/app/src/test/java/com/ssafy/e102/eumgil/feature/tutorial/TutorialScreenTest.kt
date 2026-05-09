package com.ssafy.e102.eumgil.feature.tutorial

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TutorialScreenTest {
    @Test
    fun `tutorial uses a frame-free explanation panel that fills the body area`() {
        assertEquals(360.dp, TutorialLayoutDefaults.visualPanelMaxWidth)
        assertEquals(1f, TutorialLayoutDefaults.visualPanelWeight, 0f)
        assertEquals(12.dp, TutorialLayoutDefaults.visualPanelButtonGap)
    }

    @Test
    fun `tutorial keeps the hero mark fixed without a background tile`() {
        assertEquals(false, TutorialLayoutDefaults.hasHeroIconBackground)
        assertEquals(56.dp, TutorialLayoutDefaults.heroIconSize)
        assertEquals(88.dp, TutorialLayoutDefaults.previousButtonMinWidth)
        assertEquals(2, TutorialLayoutDefaults.firstStepWithPreviousAction)
        assertEquals(1f, TutorialLayoutDefaults.panelTouchNavigationZoneWeight, 0f)
    }

    @Test
    fun `tutorial keeps concise visual cues while preserving key chips`() {
        assertEquals(3, TutorialLayoutDefaults.totalStepCount)
        assertEquals(0, TutorialLayoutDefaults.supportingItemCount)
        assertEquals(3, TutorialLayoutDefaults.destinationFilterChipCount)
        assertEquals(3, TutorialLayoutDefaults.routeAccessibilityChipCount)
        assertEquals(3, TutorialLayoutDefaults.reportCategoryChipCount)
        assertEquals(false, TutorialLayoutDefaults.showsEmphasisChip)
        assertEquals(true, TutorialLayoutDefaults.usesLayeredFlatIllustration)
    }

    @Test
    fun `tutorial uses readable header and panel text rhythm`() {
        assertEquals(34.sp, TutorialLayoutDefaults.headerHeadlineLineHeight)
        assertEquals(22.sp, TutorialLayoutDefaults.headerDescriptionLineHeight)
        assertEquals(22.dp, TutorialLayoutDefaults.illustrationContentGap)
        assertEquals(1.dp, TutorialLayoutDefaults.previousButtonBorderWidth)
    }
}
