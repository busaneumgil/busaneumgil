package com.ssafy.e102.eumgil.feature.tutorial

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TutorialScreenTest {
    @Test
    fun `tutorial uses a full body stage without a white card frame`() {
        assertEquals(420.dp, TutorialLayoutDefaults.visualPanelMaxWidth)
        assertEquals(1f, TutorialLayoutDefaults.visualPanelWeight, 0f)
        assertEquals(12.dp, TutorialLayoutDefaults.visualPanelButtonGap)
        assertEquals(false, TutorialLayoutDefaults.usesWhitePanelFrame)
        assertEquals(3, TutorialLayoutDefaults.distinctIllustrationSceneCount)
        assertEquals(true, TutorialLayoutDefaults.usesNavigationSafeZone)
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
        assertEquals(0, TutorialLayoutDefaults.routeAccessibilityChipCount)
        assertEquals(0, TutorialLayoutDefaults.reportCategoryChipCount)
        assertEquals(false, TutorialLayoutDefaults.showsEmphasisChip)
        assertEquals(true, TutorialLayoutDefaults.usesLayeredFlatIllustration)
        assertEquals(false, TutorialLayoutDefaults.usesIllustrationHalo)
        assertEquals(true, TutorialLayoutDefaults.destinationFiltersAttachToSearch)
        assertEquals(true, TutorialLayoutDefaults.destinationSearchShowsMic)
        assertEquals(false, TutorialLayoutDefaults.destinationShowsMapPreview)
    }

    @Test
    fun `tutorial uses readable header and panel text rhythm`() {
        assertEquals(34.sp, TutorialLayoutDefaults.headerHeadlineLineHeight)
        assertEquals(22.sp, TutorialLayoutDefaults.headerDescriptionLineHeight)
        assertEquals(12.dp, TutorialLayoutDefaults.illustrationContentGap)
        assertEquals(6.dp, TutorialLayoutDefaults.headerVisualGap)
        assertEquals(1.dp, TutorialLayoutDefaults.previousButtonBorderWidth)
    }
}
