package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.feature.navigation.component.createNavigationSegmentRailSlots
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationSegmentRailLayoutTest {
    @Test
    fun `rail slots move first and last segments into waypoint slots`() {
        val slots =
            createNavigationSegmentRailSlots(
                NavigationSegmentSyncUiState(
                    railItems =
                        listOf(
                            railItem(index = 0, sequence = 1),
                            railItem(index = 1, sequence = 2),
                            railItem(index = 2, sequence = 3),
                            railItem(index = 3, sequence = 4),
                        ),
                ),
            )

        assertEquals(0, slots.originItem?.index)
        assertEquals(3, slots.destinationItem?.index)
        assertEquals(listOf(1, 2), slots.intermediateItems.map { item -> item.index })
        assertFalse(slots.canReturnToActiveSegment)
    }

    @Test
    fun `rail slots keep single segment available on both waypoint slots`() {
        val slots =
            createNavigationSegmentRailSlots(
                NavigationSegmentSyncUiState(
                    activeSegmentIndex = 0,
                    focusedSegmentIndex = 0,
                    railItems = listOf(railItem(index = 0, sequence = 1, isActive = true)),
                ),
            )

        assertEquals(0, slots.originItem?.index)
        assertEquals(0, slots.destinationItem?.index)
        assertTrue(slots.intermediateItems.isEmpty())
    }

    @Test
    fun `rail slots keep return action enabled while inspecting a moved waypoint segment`() {
        val slots =
            createNavigationSegmentRailSlots(
                NavigationSegmentSyncUiState(
                    activeSegmentIndex = 0,
                    focusedSegmentIndex = 2,
                    isInspectingSegments = true,
                    railItems =
                        listOf(
                            railItem(index = 0, sequence = 1, isActive = true),
                            railItem(index = 1, sequence = 2),
                            railItem(index = 2, sequence = 3, isFocused = true),
                        ),
                ),
            )

        assertTrue(slots.canReturnToActiveSegment)
        assertEquals(2, slots.destinationItem?.index)
    }

    @Test
    fun `rail source scrolls to the newly focused segment`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/component/NavigationSegmentRail.kt")
                .readText()

        assertTrue(source.contains("rememberLazyListState()"))
        assertTrue(source.contains("LaunchedEffect(uiState.focusedSegmentIndex, uiState.railItems.size)"))
        assertTrue(source.contains("listState.animateScrollToItem(targetItemIndex)"))
    }
}

private fun railItem(
    index: Int,
    sequence: Int,
    isActive: Boolean = false,
    isFocused: Boolean = false,
): NavigationSegmentRailItemUiState =
    NavigationSegmentRailItemUiState(
        index = index,
        sequence = sequence,
        instruction = "Segment $sequence",
        distanceLabel = "${sequence * 100}m",
        riskLabel = "낮음",
        isActive = isActive,
        isFocused = isFocused,
    )
