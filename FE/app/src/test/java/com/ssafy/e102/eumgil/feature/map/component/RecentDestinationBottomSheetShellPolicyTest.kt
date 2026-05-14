package com.ssafy.e102.eumgil.feature.map.component

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentDestinationBottomSheetShellPolicyTest {
    private val source =
        File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/RecentDestinationBottomSheetShell.kt")
            .readText()

    @Test
    fun `recent destination sheet header keeps a compact title and emphasized view all action`() {
        assertTrue(
            "Recent destination header title should use a more compact typography level than titleLarge.",
            source.contains("style = MaterialTheme.typography.titleMedium"),
        )
        assertTrue(
            "View-all CTA should keep the requested Korean label.",
            source.contains("text = \"전체보기\""),
        )
        assertTrue(
            "View-all CTA should include a chevron-like greater-than marker.",
            source.contains("text = \">\""),
        )
        assertTrue(
            "View-all CTA should use the design convention blue accent token.",
            source.contains("color = MaterialTheme.colorScheme.secondary"),
        )
    }

    @Test
    fun `recent destination row uses a larger bare icon without a tinted background chip`() {
        assertTrue(
            "Recent destination rows should enlarge the category icon to the large icon size.",
            source.contains(".size(36.dp)"),
        )
        assertFalse(
            "Recent destination icons should no longer sit on a tinted background surface.",
            source.contains("color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)"),
        )
    }

    @Test
    fun `recent destination navigation actions suppress ripple while keeping the sheet animation`() {
        assertTrue(
            "Recent destination sheet should keep AnimatedVisibility for the bottom sheet slide motion.",
            source.contains("AnimatedVisibility("),
        )
        assertTrue(
            "View-all navigation action should suppress ripple because it opens the saved-route screen.",
            source.contains("private fun RecentDestinationViewAllAction(") &&
                source.contains("indication = null"),
        )
        assertTrue(
            "Route CTA should suppress ripple because it opens route setting from the map sheet.",
            source.contains("private fun RecentDestinationRouteButton(") &&
                source.contains("indication = null"),
        )
    }

    @Test
    fun `recent destination sheet leaves a restore handle after user dismissal`() {
        assertTrue(
            "Recent destination dismissal should switch to a compact restore handle instead of making the sheet unreachable.",
            source.contains("val isRestoreHandleVisible = state.isVisible && isDismissedByUser") &&
                source.contains("RecentDestinationRestoreHandle("),
        )
        assertTrue(
            "The restore handle should support both tap and upward drag to reopen recent destinations.",
            source.contains("isDismissedByUser = false") &&
                source.contains("if (delta < 0f)") &&
                source.contains("map_recent_destination_sheet_restore"),
        )
    }
}
