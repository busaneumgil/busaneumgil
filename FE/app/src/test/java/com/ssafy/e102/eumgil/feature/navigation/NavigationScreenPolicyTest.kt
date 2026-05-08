package com.ssafy.e102.eumgil.feature.navigation

import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.core.model.RouteOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `hero layout policy uses taller blue header and removes divider`() {
        val policy = navigationHeroLayoutPolicy(800.dp)

        assertEquals(116.dp, policy.minHeight)
        assertEquals(192.dp, policy.maxHeight)
        assertFalse(policy.showBottomDivider)
    }

    @Test
    fun `hero layout policy keeps a taller minimum max height on compact screens`() {
        val policy = navigationHeroLayoutPolicy(480.dp)

        assertEquals(132.dp, policy.maxHeight)
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
