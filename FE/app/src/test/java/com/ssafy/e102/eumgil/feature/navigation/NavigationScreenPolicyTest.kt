package com.ssafy.e102.eumgil.feature.navigation

import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.core.model.RouteOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NavigationScreenPolicyTest {
    @Test
    fun `screen policy hides rail and return action when no segment sync data exists`() {
        val policy = navigationScreenPolicy(NavigationUiState())

        assertFalse(policy.showSegmentRail)
        assertFalse(policy.showReturnToActiveAction)
        assertFalse(policy.showFocusedSegmentCard)
    }

    @Test
    fun `screen policy shows rail and return action during inspect mode`() {
        val uiState =
            NavigationUiState(
                segmentSync =
                    NavigationSegmentSyncUiState(
                        activeSegmentIndex = 0,
                        focusedSegmentIndex = 1,
                        isInspectingSegments = true,
                        railItems =
                            listOf(
                                NavigationSegmentRailItemUiState(
                                    index = 0,
                                    sequence = 1,
                                    instruction = "Walk straight",
                                    distanceLabel = "120m",
                                    riskLabel = "Low",
                                    isActive = true,
                                ),
                                NavigationSegmentRailItemUiState(
                                    index = 1,
                                    sequence = 2,
                                    instruction = "Cross the street",
                                    distanceLabel = "80m",
                                    riskLabel = "Medium",
                                    isFocused = true,
                                ),
                            ),
                    ),
                    focusedSegmentCard =
                        NavigationFocusedSegmentCardUiState(
                            sequenceLabel = "2 / 2",
                            instruction = "Cross the street",
                            heroTitle = "횡단보도 건너기",
                            heroDescription = "Cross the street",
                            distanceLabel = "80m",
                            riskLabel = "Medium",
                            supportingText = "Focused segment details",
                        ),
            )

        val policy = navigationScreenPolicy(uiState)

        assertTrue(policy.showSegmentRail)
        assertTrue(policy.showReturnToActiveAction)
        assertFalse(policy.showFocusedSegmentCard)
    }

    @Test
    fun `screen policy keeps rail visible when detail action must remain reachable`() {
        val policy =
            navigationScreenPolicy(
                NavigationUiState(
                    screenState = NavigationScreenState.Empty,
                    selectedRouteOption = RouteOption.SAFE,
                ),
            )

        assertTrue(policy.showSegmentRail)
    }

    @Test
    fun `hero layout policy uses compact current guidance card and removes divider`() {
        val policy = navigationHeroLayoutPolicy(800.dp)

        assertEquals(92.dp, policy.minHeight)
        assertEquals(128.dp, policy.maxHeight)
        assertEquals(44.dp, policy.directionIconSize)
        assertFalse(policy.showBottomDivider)
    }

    @Test
    fun `hero layout policy keeps compact minimum max height on compact screens`() {
        val policy = navigationHeroLayoutPolicy(480.dp)

        assertEquals(108.dp, policy.maxHeight)
    }

    @Test
    fun `transit icons use slightly smaller hero and rail sizes than turn guidance icons`() {
        val navigationScreenSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt")
                .readText()
        val railSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/component/NavigationSegmentRail.kt")
                .readText()
        val guideSidePanelSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/guidance/component/GuideSidePanel.kt")
                .readText()

        assertTrue(
            "Navigation hero icon sizing should route transit actions through a dedicated helper.",
            navigationScreenSource.contains("modifier = Modifier.size(guidanceAction.heroIconSize(defaultSize = iconSize))"),
        )
        assertTrue(
            "Navigation hero should document the slightly reduced transit icon token.",
            navigationScreenSource.contains("private val NavigationHeroTransitDirectionIconSize = 40.dp"),
        )
        assertTrue(
            "Navigation segment rail should delegate collapsed icon sizing to the shared guide side-panel item.",
            railSource.contains("GuideCollapsedRailItem(") &&
                guideSidePanelSource.contains("action.collapsedIconSize()"),
        )
        assertTrue(
            "Shared collapsed rail icons should document the slightly reduced transit icon token.",
            guideSidePanelSource.contains("private val GuideCollapsedRailTransitIconSize = 22.dp"),
        )
    }

    @Test
    fun `navigation screen provides expandable side panel over map for walk and transit guidance`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt")
                .readText()
        val railSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/component/NavigationSegmentRail.kt")
                .readText()

        assertTrue(
            "NAV-01 should keep the map as the base layer while the left rail or panel overlays it.",
            source.contains("NavigationMapStage(") &&
                source.contains("NavigationExpandedSidePanel(") &&
                source.contains("NavigationSegmentRail("),
        )
        assertTrue(
            "The side panel should support horizontal swipe collapse.",
            source.contains("GuideSidePanelShell("),
        )
        assertTrue(
            "Rows should collapse the panel and reuse SegmentTapped so map focus stays on the existing ViewModel path.",
            source.contains("isSidePanelExpanded = false") &&
                source.contains("NavigationUiAction.SegmentTapped(index = index)"),
        )
        assertTrue(
            "Transit guidance actions should use the same side panel row path as walk guidance.",
            source.contains("GuideSidePanelStepRow(") &&
                source.contains("uiState.segmentSync.railItems.forEach"),
        )
        assertTrue(
            "Collapsed rail vertical scroll should reuse SegmentTapped so the top visible icon drives the hero card and map focus.",
            source.contains("onTopVisibleSegmentChanged = { index ->") &&
                railSource.contains("snapshotFlow") &&
                railSource.contains("firstVisibleItemIndex") &&
                railSource.contains("onTopVisibleSegmentChanged"),
        )
        assertFalse(
            "Expanded side panel rows should not keep the radio-like current-location button.",
            source
                .substringAfter("private fun NavigationSidePanelRow(")
                .substringBefore("private fun navigationRouteSummary")
                .contains("ic_map_current_location"),
        )
        assertFalse(
            "Expanded side panel should not render a duplicate progress header.",
            source
                .substringAfter("private fun NavigationExpandedSidePanel(")
                .substringBefore("@Composable\nprivate fun NavigationSidePanelRow")
                .contains("navigationRouteSummary(uiState)"),
        )
    }

    @Test
    fun `navigation side panel and exit CTA match route detail reference shell`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt")
                .readText()
        val guideSidePanelSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/guidance/component/GuideSidePanel.kt")
                .readText()
        val expandedPanelSection =
            source
                .substringAfter("private fun NavigationExpandedSidePanel(")
                .substringBefore("@Composable\nprivate fun NavigationSidePanelRow")
        val rowSection =
            source
                .substringAfter("private fun NavigationSidePanelRow(")
                .substringBefore("private fun navigationRouteSummary")
        val bottomBarSection =
            source
                .substringAfter("private fun NavigationBottomBar(")
                .substringBefore("@Composable\nprivate fun NavigationMapStage")

        assertTrue(
            "Expanded guidance panel should use the dimmed map scrim shown in the reference side-sheet state.",
            source.contains("NavigationExpandedSidePanelScrimColor"),
        )
        assertTrue(
            "Expanded panel rows should use the shared start/end and turn icon row as the collapsed rail.",
            expandedPanelSection.contains("isFirst = index == 0") &&
                expandedPanelSection.contains("isLast = index == uiState.segmentSync.railItems.lastIndex") &&
                rowSection.contains("GuideSidePanelStepRow(") &&
                guideSidePanelSource.contains("fun GuideSidePanelStepIcon("),
        )
        assertTrue(
            "Exit CTA should share the full-width bottom placement and 30dp bottom gap used by route start.",
            bottomBarSection.contains(".fillMaxWidth()") &&
                bottomBarSection.contains("bottom = NavigationBottomBarBottomGap") &&
                source.contains("NavigationBottomBarBottomGap = 30.dp"),
        )
        assertFalse(
            "Exit CTA should not add navigation bar inset on top of the explicit 30dp app-bottom gap.",
            bottomBarSection.contains(".navigationBarsPadding()"),
        )
        assertFalse(
            "Exit CTA text should not be squeezed by the old fixed-width button.",
            bottomBarSection.contains(".width(NavigationBottomBarButtonWidth)"),
        )
        assertTrue(
            "Exit CTA should use the same 50dp narrower horizontal inset as the route start CTA.",
            source.contains("NavigationBottomBarHorizontalPadding = EumSpacing.medium + 50.dp") &&
                bottomBarSection.contains("start = NavigationBottomBarHorizontalPadding") &&
                bottomBarSection.contains("end = NavigationBottomBarHorizontalPadding"),
        )
    }

    @Test
    fun `hero content prioritizes focused segment guidance over the active step card`() {
        val heroContent =
            navigationHeroContent(
                NavigationUiState(
                    stepCard =
                        NavigationStepCardUiState(
                            heroTitle = "직진 이동",
                            heroDescription = "Proceed straight on the current route",
                            instruction = "Proceed straight on the current route",
                            distanceLabel = "120m",
                            guidanceAction = NavigationGuidanceAction.STRAIGHT,
                        ),
                    focusedSegmentCard =
                        NavigationFocusedSegmentCardUiState(
                            sequenceLabel = "2 / 3",
                            instruction = "Cross the street and head toward the elevator",
                            heroTitle = "횡단보도 건너기",
                            heroDescription = "Cross the street and head toward the elevator",
                            distanceLabel = "80m",
                            riskLabel = "Low",
                            supportingText = "Focused segment",
                            guidanceAction = NavigationGuidanceAction.CROSSWALK,
                        ),
                ),
            )

        assertEquals(NavigationGuidanceAction.CROSSWALK, heroContent.guidanceAction)
        assertEquals("횡단보도 건너기", heroContent.title)
        assertEquals("Cross the street and head toward the elevator", heroContent.description)
        assertEquals("80m", heroContent.distanceLabel)
    }

    @Test
    fun `navigation hero animates focused card changes and does not pin transit card during inspection`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt")
                .readText()
        val heroSection =
            source
                .substringAfter("private fun NavigationHeroCard(")
                .substringBefore("@Composable\nprivate fun NavigationHeroDirectionIcon")

        assertTrue(
            "Hero content changes should use vertical AnimatedContent so scroll-driven guidance changes are visible.",
            heroSection.contains("AnimatedContent(") &&
                heroSection.contains("slideInVertically") &&
                heroSection.contains("slideOutVertically"),
        )
        assertTrue(
            "Focused rail inspection should show transit detail only when the focused segment itself is bus/subway.",
            heroSection.contains("focusedSegmentCard?.transitInfo"),
        )
        assertFalse(
            "The active step transit summary must not pin the top card during normal entry or inspection.",
            heroSection.contains("uiState.stepCard.transitInfo"),
        )
    }

    @Test
    fun `hero content falls back to the active step card when no focused segment is open`() {
        val heroContent =
            navigationHeroContent(
                NavigationUiState(
                    stepCard =
                        NavigationStepCardUiState(
                            heroTitle = "우회전",
                            heroDescription = "Turn right after the crosswalk",
                            instruction = "Turn right after the crosswalk",
                            distanceLabel = "240m",
                            guidanceAction = NavigationGuidanceAction.TURN_RIGHT,
                        ),
                ),
            )

        assertEquals(NavigationGuidanceAction.TURN_RIGHT, heroContent.guidanceAction)
        assertEquals("우회전", heroContent.title)
        assertEquals("Turn right after the crosswalk", heroContent.description)
        assertEquals("240m", heroContent.distanceLabel)
    }

    @Test
    fun `bottom bar layout policy starts divider after rail when segment rail is visible`() {
        val policy =
            navigationBottomBarLayoutPolicy(
                showSegmentRail = true,
                railWidth = 56.dp,
            )

        assertEquals(56.dp, policy.topDividerStartInset)
    }

    @Test
    fun `bottom bar layout policy keeps full width divider when segment rail is hidden`() {
        val policy =
            navigationBottomBarLayoutPolicy(
                showSegmentRail = false,
                railWidth = 56.dp,
            )

        assertEquals(0.dp, policy.topDividerStartInset)
    }
}
