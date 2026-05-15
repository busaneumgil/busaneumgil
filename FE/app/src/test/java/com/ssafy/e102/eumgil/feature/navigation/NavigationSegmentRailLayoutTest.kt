package com.ssafy.e102.eumgil.feature.navigation

import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.feature.navigation.component.createNavigationSegmentRailSlots
import com.ssafy.e102.eumgil.feature.navigation.component.resolveGuideRailAutoScrollItemIndex
import com.ssafy.e102.eumgil.feature.navigation.component.resolveGuideRailEndSnapPadding
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
        assertTrue(slots.canScrollToTop)
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
    fun `rail slots keep top action enabled while inspecting a moved waypoint segment`() {
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

        assertTrue(slots.canScrollToTop)
        assertEquals(2, slots.destinationItem?.index)
    }

    @Test
    fun `collapsed rail paints a surface like route detail rail instead of bleeding into the map`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/component/NavigationSegmentRail.kt")
                .readText()
        val railSection =
            source
                .substringAfter("fun NavigationSegmentRail(")
                .substringBefore("@Composable\nprivate fun NavigationSegmentRailWaypoint")

        assertTrue(railSection.contains(".background(MaterialTheme.colorScheme.surface)"))
        assertTrue(railSection.contains(".fillMaxHeight()"))
    }

    @Test
    fun `rail top action stays enabled and scrolls to the first item`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/component/NavigationSegmentRail.kt")
                .readText()

        assertTrue(source.contains("R.string.navigation_rail_scroll_to_top_label"))
        assertTrue(source.contains("listState.animateScrollToItem(0, scrollOffset = 0)"))
        assertTrue(source.contains("listState.scrollToItem(0, scrollOffset = 0)"))
    }

    @Test
    fun `rail removes the bottom route detail more action`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/component/NavigationSegmentRail.kt")
                .readText()
        val railSection =
            source
                .substringAfter("fun NavigationSegmentRail(")
                .substringBefore("@Composable\nprivate fun NavigationSegmentRailWaypoint")

        assertFalse(railSection.contains("NavigationSegmentRailDetailAction("))
        assertFalse(source.contains("ic_navigation_detail_more"))
    }

    @Test
    fun `rail source snaps the promoted segment to the top`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/component/NavigationSegmentRail.kt")
                .readText()
        val collectSection =
            source
                .substringAfter(".collect { snapshot ->")
                .substringBefore("LaunchedEffect(uiState.focusedSegmentIndex")

        assertTrue(source.contains("rememberLazyListState()"))
        assertTrue(source.contains("snapshotFlow"))
        assertTrue(source.contains("contentPadding = PaddingValues(bottom = navigationRailEndSnapPadding)"))
        assertTrue(source.contains("listState.animateScrollToItem(position, scrollOffset = 0)"))
        assertTrue(source.contains("listState.scrollToItem(position, scrollOffset = 0)"))
        assertTrue(source.contains("NavigationSegmentRailItemHeight = 96.dp"))
        assertTrue(
            "A fast fling should update the promoted item while scrolling so the top card does not temporarily lose its guidance.",
            collectSection.contains("val isSettlingAfterCollapsedTopCard") &&
                collectSection.contains("if (!isSettlingAfterCollapsedTopCard") &&
                collectSection.contains("hiddenRailItemPosition = promotedItemPosition"),
        )
        assertTrue(
            "The rail should snap and hide the promoted slot before it notifies the top card, preventing fast fling recomposition from interrupting the snap.",
            collectSection.indexOf("snapshot.shouldSnapToPromotedItem()") <
                collectSection.indexOf("currentOnTopVisibleSegmentChanged"),
        )
        assertTrue(
            "The scroll-to-top action should promote the first guide card and keep that first icon out of the collapsed rail.",
            source.contains("hiddenRailItemPosition = 0") &&
                source.contains("railFocusItems.firstOrNull()?.index?.let(onSegmentTapped)"),
        )
    }

    @Test
    fun `rail external selection scrolls the next icon to the top while clamping the destination`() {
        assertEquals(1, resolveGuideRailAutoScrollItemIndex(focusedItemPosition = 0, itemCount = 4))
        assertEquals(3, resolveGuideRailAutoScrollItemIndex(focusedItemPosition = 2, itemCount = 4))
        assertEquals(3, resolveGuideRailAutoScrollItemIndex(focusedItemPosition = 3, itemCount = 4))
        assertEquals(null, resolveGuideRailAutoScrollItemIndex(focusedItemPosition = -1, itemCount = 4))
        assertEquals(null, resolveGuideRailAutoScrollItemIndex(focusedItemPosition = 0, itemCount = 0))
    }

    @Test
    fun `rail end padding lets destination reach the top but keeps the scroll top action below it`() {
        assertEquals(
            448.dp,
            resolveGuideRailEndSnapPadding(
                viewportHeight = 600.dp,
                guideItemHeight = 96.dp,
                trailingActionHeight = 56.dp,
            ),
        )
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
