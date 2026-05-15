package com.ssafy.e102.eumgil.feature.navigation

import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.feature.navigation.component.NavigationSegmentRailTransitIconSize
import com.ssafy.e102.eumgil.feature.navigation.component.railIconSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.File
import org.junit.Test

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
        assertEquals(NavigationHeroTransitDirectionIconSize, NavigationGuidanceAction.BUS.heroIconSize(defaultSize = 44.dp))
        assertEquals(NavigationHeroTransitDirectionIconSize, NavigationGuidanceAction.SUBWAY.heroIconSize(defaultSize = 44.dp))
        assertEquals(44.dp, NavigationGuidanceAction.STRAIGHT.heroIconSize(defaultSize = 44.dp))
        assertEquals(NavigationSegmentRailTransitIconSize, NavigationGuidanceAction.BUS.railIconSize())
        assertTrue(NavigationGuidanceAction.STRAIGHT.railIconSize() > NavigationGuidanceAction.BUS.railIconSize())
    }

    @Test
    fun `navigation screen provides expandable side panel over map for walk and transit guidance`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt")
                .readText()
        val sidePanelPolicy = navigationSidePanelPolicy()

        assertTrue(
            "NAV-01 should keep the map as the base layer while the left rail or panel overlays it.",
            source.contains("NavigationMapStage(") &&
                source.contains("NavigationExpandedSidePanel(") &&
                source.contains("NavigationSegmentRail("),
        )
        assertEquals(NavigationSidePanelSwipeAxis.Horizontal, sidePanelPolicy.swipeAxis)
        assertEquals(80f, sidePanelPolicy.swipeThresholdPx, 0f)
        assertTrue(sidePanelPolicy.collapseOnSegmentTap)
        assertTrue(
            "Transit guidance actions should use the same side panel row path as walk guidance.",
            source.contains("item.guidanceAction.iconRes()") &&
                source.contains("uiState.segmentSync.railItems.forEach"),
        )
        assertFalse(
            "Expanded side panel rows should not keep the radio-like current-location button.",
            source
                .substringAfter("private fun NavigationSidePanelRow(")
                .substringBefore("@Composable\nprivate fun NavigationSidePanelExpandHandle")
                .contains("ic_map_current_location"),
        )
        assertFalse(sidePanelPolicy.showsProgressHeader)
    }

    @Test
    fun `navigation side panel and exit CTA match route detail reference shell`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt")
                .readText()
        val sidePanelPolicy = navigationSidePanelPolicy()
        val bottomBarChromePolicy = navigationBottomBarChromePolicy()

        assertTrue(sidePanelPolicy.showsExpandedScrim)
        assertTrue(
            "Expanded panel rows should use the same start/end and turn icons as the collapsed rail.",
            source.contains("isFirst = index == 0") &&
                source.contains("isLast = index == uiState.segmentSync.railItems.lastIndex") &&
                source.contains("NavigationSidePanelStepIcon("),
        )
        assertEquals(30.dp, bottomBarChromePolicy.bottomGap)
        assertTrue(bottomBarChromePolicy.usesNavigationBarPadding)
        assertFalse(
            "Exit CTA text should not be squeezed by the old fixed-width button.",
            source.contains(".width(NavigationBottomBarButtonWidth)"),
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

    @Test
    fun `exit dialog policy follows navigation design tokens`() {
        val policy = navigationExitDialogPolicy()

        assertEquals(NavigationExitDialogShell.Dialog, policy.shell)
        assertEquals(360.dp, policy.maxWidth)
        assertEquals(EumRadius.scaleL, policy.containerCornerRadius)
        assertEquals(EumRadius.scaleM, policy.buttonCornerRadius)
        assertEquals(48.dp, policy.primaryButtonHeight)
        assertEquals(44.dp, policy.secondaryButtonHeight)
        assertEquals(10.dp, policy.shadowElevation)
    }

    @Test
    fun `exit dialog uses custom dialog shell instead of default alert dialog`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt")
                .readText()
        val dialogSection =
            source
                .substringAfter("private fun NavigationExitConfirmDialog(")
                .substringBefore("private fun DrawScope.drawNavigationMapGrid")

        assertTrue(dialogSection.contains("Dialog("))
        assertTrue(dialogSection.contains("navigationExitDialogPolicy()"))
        assertTrue(dialogSection.contains("BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f))"))
        assertTrue(dialogSection.contains("Arrangement.spacedBy(EumSpacing.large)"))
        assertTrue(dialogSection.contains("Arrangement.spacedBy(EumSpacing.medium)"))
        assertFalse(dialogSection.contains(".background(MaterialTheme.colorScheme.error)"))
        assertFalse(dialogSection.contains("navigation_exit_confirm_dialog_supporting"))
        assertFalse(dialogSection.contains("navigation_exit_confirm_dialog_eyebrow"))
        assertFalse(dialogSection.contains("AlertDialog("))
    }

    @Test
    fun `screen scaffold disables default window insets to avoid header gap`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt")
                .readText()
        assertTrue(navigationUsesEmptyWindowInsets())
        assertTrue(source.contains(".statusBarsPadding()"))
    }
}
