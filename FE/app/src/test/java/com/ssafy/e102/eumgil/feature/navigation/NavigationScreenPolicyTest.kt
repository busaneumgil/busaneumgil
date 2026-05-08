package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.core.model.RouteOption
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
}
