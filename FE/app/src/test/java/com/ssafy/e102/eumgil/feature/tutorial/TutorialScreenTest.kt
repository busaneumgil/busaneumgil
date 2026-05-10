package com.ssafy.e102.eumgil.feature.tutorial

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element

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
        assertEquals(true, TutorialLayoutDefaults.destinationUsesSearchAndFilterOnly)
        assertEquals(false, TutorialLayoutDefaults.destinationFilterRowScrollable)
        assertEquals(true, TutorialLayoutDefaults.destinationFilterUsesLatestMapIcons)
        assertEquals(true, TutorialLayoutDefaults.destinationFilterUsesOriginalMapChipShape)
        assertEquals(true, TutorialLayoutDefaults.destinationShowsFilterResultPanel)
        assertEquals(false, TutorialLayoutDefaults.usesGeneratedBitmapIllustration)
        assertEquals(true, TutorialLayoutDefaults.reportDescriptionMentionsRouteContribution)
    }

    @Test
    fun `tutorial uses readable header and panel text rhythm`() {
        assertEquals(34.sp, TutorialLayoutDefaults.headerHeadlineLineHeight)
        assertEquals(22.sp, TutorialLayoutDefaults.headerDescriptionLineHeight)
        assertEquals(12.dp, TutorialLayoutDefaults.illustrationContentGap)
        assertEquals(4.dp, TutorialLayoutDefaults.headerVisualGap)
        assertEquals(20.dp, TutorialLayoutDefaults.headerTopPadding)
        assertEquals(12.dp, TutorialLayoutDefaults.sceneContentVerticalGap)
        assertEquals(1.dp, TutorialLayoutDefaults.previousButtonBorderWidth)
        assertEquals(96.dp, TutorialLayoutDefaults.routeCardHeight)
        assertEquals(148.dp, TutorialLayoutDefaults.reportTileWidth)
        assertEquals(112.dp, TutorialLayoutDefaults.reportTileHeight)
    }

    @Test
    fun `destination controls sit close together with equal vertical rhythm`() {
        assertEquals(300.dp, TutorialLayoutDefaults.illustrationHeight)
        assertEquals(340.dp, TutorialLayoutDefaults.searchBarMaxWidth)
        assertEquals(12.dp, TutorialLayoutDefaults.destinationControlVerticalGap)
        assertEquals(13.dp, TutorialLayoutDefaults.filterChipHorizontalPadding)
        assertEquals(9.dp, TutorialLayoutDefaults.filterChipVerticalPadding)
        assertEquals(38.dp, TutorialLayoutDefaults.filterChipMinHeight)
        assertEquals(8.dp, TutorialLayoutDefaults.filterChipCornerRadius)
        assertEquals(2, TutorialLayoutDefaults.filterChipRowMaxItemCount)
        assertEquals(340.dp, TutorialLayoutDefaults.destinationResultPanelMaxWidth)
        assertEquals(12.dp, TutorialLayoutDefaults.destinationResultPanelGap)
    }

    @Test
    fun `route description keeps the setting phrase on its own line`() {
        assertEquals(true, TutorialLayoutDefaults.routeDescriptionBreaksAfterSettingComma)
    }

    @Test
    fun `route efficient time copy is thirty five minutes`() {
        val routeEfficientTime =
            File("src/main/res/values/strings.xml")
                .readStringResource(name = "tutorial_route_efficient_time")

        assertEquals("35분", routeEfficientTime)
    }

    private fun File.readStringResource(name: String): String {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(this)
        val strings = document.getElementsByTagName("string")

        return (0 until strings.length)
            .asSequence()
            .map { strings.item(it) as Element }
            .first { it.getAttribute("name") == name }
            .textContent
    }
}
