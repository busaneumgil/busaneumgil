package com.ssafy.e102.eumgil.feature.savedroute

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedRouteScreenPolicyTest {
    @Test
    fun `saved route navigate button uses provided png icon without tint override`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/savedroute/SavedRouteScreen.kt")
                .readText()
        val asset = File("src/main/res/drawable/ic_route_start_navigation_button.png")

        assertTrue(
            "Saved route navigate button should use the provided PNG icon asset for the button.",
            source.contains("R.drawable.ic_route_start_navigation_button"),
        )
        assertTrue(
            "Saved route navigate button should tint the icon blue on the white button background.",
            source.contains("tint = MaterialTheme.colorScheme.primary"),
        )
        assertTrue("Saved route navigate button PNG icon should exist in drawable.", asset.exists())
    }

    @Test
    fun `saved place rows suppress ripple when tapping through to the map screen`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/savedroute/SavedRouteScreen.kt")
                .readText()
        val savedPlaceSection =
            source
                .substringAfter("private fun SavedPlaceListItem(")
                .substringBefore("@Composable\nprivate fun SavedRouteBookmarkListItem")

        assertTrue(
            "Saved place rows should suppress ripple because the content tap navigates back to the map.",
            savedPlaceSection.contains("indication = null"),
        )
        assertTrue(
            "Saved place rows should keep a dedicated interaction source when ripple is suppressed.",
            savedPlaceSection.contains("MutableInteractionSource()"),
        )
    }

    @Test
    fun `saved place category icon stays larger and vertically centered`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/savedroute/SavedRouteScreen.kt")
                .readText()
        val savedPlaceSection =
            source
                .substringAfter("private fun SavedPlaceListItem(")
                .substringBefore("@Composable\nprivate fun SavedRouteBookmarkListItem")

        assertTrue(
            "Saved-place category icon row should center the icon against the full bookmark text stack.",
            savedPlaceSection.contains("verticalAlignment = Alignment.CenterVertically"),
        )
        assertTrue(
            "Saved-place category icon should also declare its own vertical centering inside the row.",
            savedPlaceSection.contains(".align(Alignment.CenterVertically)"),
        )
        assertTrue(
            "Saved-place category icon token should stay noticeably larger than the previous compact size.",
            source.contains("private val SavedBookmarkCategoryIconSize = 40.dp"),
        )
    }

    @Test
    fun `saved bookmark names use compact wrapping rules`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/savedroute/SavedRouteScreen.kt")
                .readText()
        val savedPlaceSection =
            source
                .substringAfter("private fun SavedPlaceListItem(")
                .substringBefore("@Composable\nprivate fun SavedRouteBookmarkListItem")
        val routeBookmarkSection =
            source
                .substringAfter("private fun SavedRouteBookmarkListItem(")
                .substringBefore("private fun SavedBookmarkSelectionButton(")
        val routeWaypointInfoSection =
            source
                .substringAfter("private fun SavedRouteWaypointInfoRow(")
                .substringBefore("private fun routeOptionCompactLabel(")

        assertTrue(
            "Saved-place names should use a dedicated compact line-height token instead of the default title spacing.",
            savedPlaceSection.contains("SavedBookmarkPlaceNameLineHeight"),
        )
        assertTrue(
            "Saved-place names should clamp to two lines inside bookmark cards.",
            savedPlaceSection.contains("maxLines = SavedBookmarkPrimaryTextMaxLines"),
        )
        assertTrue(
            "Saved-place names should end with ellipsis when the card width is too narrow.",
            savedPlaceSection.contains("overflow = TextOverflow.Ellipsis"),
        )
        assertTrue(
            "Saved-route waypoint values should use a dedicated compact line-height token instead of the default body spacing.",
            routeWaypointInfoSection.contains("SavedBookmarkWaypointValueLineHeight"),
        )
        assertTrue(
            "Saved-route waypoint values should stay on a single line inside bookmark cards.",
            routeWaypointInfoSection.contains("maxLines = SavedBookmarkWaypointValueMaxLines"),
        )
        assertTrue(
            "Saved-route waypoint values should end with ellipsis when the card width is too narrow.",
            routeWaypointInfoSection.contains("overflow = TextOverflow.Ellipsis"),
        )
    }

    @Test
    fun `saved route navigation ctas suppress ripple when leaving the bookmark screen`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/savedroute/SavedRouteScreen.kt")
                .readText()
        val stateActionsSection =
            source
                .substringAfter("private fun SavedBookmarkStateActions(")
                .substringBefore("@Composable\nprivate fun SavedRouteInlineMessage")
        val primaryActionSection =
            source
                .substringAfter("private fun SavedBookmarkPrimaryActionButton(")
                .substringBefore("@Composable\nprivate fun SavedRoutePathDecoration")
        val editBottomBarSection =
            source
                .substringAfter("private fun SavedBookmarkEditBottomBar(")
                .substringBefore("@Composable\nprivate fun SavedBookmarkPrimaryActionButton")

        assertTrue(
            "Saved-route empty and error CTAs should use a no-ripple navigation button because they jump back to the map screen.",
            stateActionsSection.contains("NoRippleSavedRouteNavigationButton("),
        )
        assertTrue(
            "Saved-route empty and error CTAs should center content across the weighted button width.",
            stateActionsSection.contains("fullWidthContent = true"),
        )
        assertTrue(
            "Saved-route primary CTA should use the no-ripple navigation button when it opens route guidance.",
            primaryActionSection.contains("NoRippleSavedRouteNavigationButton("),
        )
        assertTrue(
            "Saved-route primary CTA should use a less-rounded shape than the old pill button.",
            primaryActionSection.contains("val navigationButtonShape = RoundedCornerShape(EumRadius.small)"),
        )
        assertTrue(
            "Saved-route primary CTA should reduce its minimum height slightly inside bookmark rows.",
            primaryActionSection.contains("sharedModifier.heightIn(min = 38.dp)"),
        )
        assertTrue(
            "Saved-route primary CTA should tighten horizontal and vertical padding to read smaller.",
            primaryActionSection.contains("contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)"),
        )
        assertTrue(
            "Saved-route list action buttons should avoid forcing full-width content inside list rows.",
            !primaryActionSection.contains("fullWidthContent = true"),
        )
        assertTrue(
            "Saved-route edit mode should use a sticky bottom delete CTA instead of per-card delete buttons.",
            editBottomBarSection.contains("R.string.saved_route_delete_selected") &&
                source.contains("SavedRouteUiAction.DeleteSelectedClicked"),
        )
        assertTrue(
            "Saved-route no-ripple CTA helper should disable ripple indication explicitly.",
            source.contains("indication = null"),
        )
        assertTrue(
            "Saved-route no-ripple CTA helper should keep a dedicated interaction source.",
            source.contains("MutableInteractionSource()"),
        )
    }

    @Test
    fun `route tab empty state opens route setting instead of map exploration`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/savedroute/SavedRouteScreen.kt")
                .readText()
        val routeContentSection =
            source
                .substringAfter("private fun SavedRouteBookmarkContent(")
                .substringBefore("@Composable\nprivate fun SavedBookmarkStateCard")

        assertTrue(
            "Saved route tab empty/error CTA should use route-setting wording.",
            routeContentSection.contains("R.string.saved_route_route_setting_action"),
        )
        assertTrue(
            "Saved route tab empty/error CTA should navigate to route setting, not map exploration.",
            routeContentSection.contains("SavedRouteUiAction.RouteSettingClicked"),
        )
    }

    @Test
    fun `saved route shows labeled origin and destination rows with stretched path decoration`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/savedroute/SavedRouteScreen.kt")
                .readText()
        val routeBookmarkSection =
            source
                .substringAfter("private fun SavedRouteBookmarkListItem(")
                .substringBefore("private fun SavedBookmarkSelectionButton(")
        val pathDecorationSection =
            source
                .substringAfter("private fun SavedRoutePathDecoration(")
                .substringBefore("@Composable\nprivate fun SavedRouteTagChip")

        assertTrue(
            "Saved-route cards should render a dedicated origin label row.",
            routeBookmarkSection.contains("R.string.route_setting_origin_label"),
        )
        assertTrue(
            "Saved-route cards should render a dedicated destination label row.",
            routeBookmarkSection.contains("R.string.route_setting_destination_label"),
        )
        assertFalse(
            "Saved-route cards should remove the large route-name headline once the labeled origin and destination rows are shown.",
            Regex("""\btext\s*=\s*routeBookmark\.routeName""").containsMatchIn(routeBookmarkSection),
        )
        assertTrue(
            "Saved-route cards should group the left path decoration with the waypoint rows so the decoration can match the content height.",
            routeBookmarkSection.contains(".height(IntrinsicSize.Min)"),
        )
        assertTrue(
            "The left path decoration should fill the available waypoint height instead of keeping a fixed connector segment.",
            routeBookmarkSection.contains(".fillMaxHeight()"),
        )
        assertTrue(
            "The left path decoration should stretch its connector using weight between origin and destination markers.",
            pathDecorationSection.contains(".weight(1f)"),
        )
        assertTrue(
            "Saved-route card body should align route content to the top so the destination row does not drift downward.",
            routeBookmarkSection.contains("verticalAlignment = Alignment.Top"),
        )
        assertFalse(
            "Saved-route cards should not keep the old raw one-line summary once labeled rows are shown.",
            routeBookmarkSection.contains("R.string.saved_route_route_summary"),
        )
    }

    @Test
    fun `saved route cards omit distance and duration meta labels`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/savedroute/SavedRouteScreen.kt")
                .readText()
        val routeBookmarkSection =
            source
                .substringAfter("private fun SavedRouteBookmarkListItem(")
                .substringBefore("private fun SavedBookmarkSelectionButton(")

        assertFalse(
            "Saved-route cards should not render a distance meta line once detail handles that information.",
            routeBookmarkSection.contains("savedRouteMetaLabel(routeBookmark)"),
        )
        assertFalse(
            "Saved-route cards should not render duration labels such as 약 18분 in the compact bookmark row.",
            routeBookmarkSection.contains("durationMinutes") ||
                routeBookmarkSection.contains("saved_route_meta_duration"),
        )
        assertTrue(
            "Saved-route cards should only attach compact safe or fast route labels next to the transport mode.",
            routeBookmarkSection.contains("routeOptionCompactLabel(") &&
                source.contains("saved_route_route_option_safe_compact") &&
                source.contains("saved_route_route_option_fast_compact"),
        )
    }

    @Test
    fun `saved route card body opens route detail on tap`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/savedroute/SavedRouteScreen.kt")
                .readText()
        val routeContentSection =
            source
                .substringAfter("private fun SavedRouteBookmarkContent(")
                .substringBefore("@Composable\nprivate fun SavedBookmarkStateCard")
        val routeBookmarkSection =
            source
                .substringAfter("private fun SavedRouteBookmarkListItem(")
                .substringBefore("private fun SavedBookmarkSelectionButton(")

        assertTrue(
            "Saved-route list should dispatch a dedicated route-card tap action instead of reusing only the start button action.",
            routeContentSection.contains("SavedRouteUiAction.RouteClicked(bookmarkId = routeBookmark.bookmarkId)"),
        )
        assertTrue(
            "Saved-route card body should expose its own click callback so the bookmark summary opens route detail.",
            routeBookmarkSection.contains("onRouteClick: (() -> Unit)?"),
        )
        assertTrue(
            "Saved-route card body should suppress ripple when tapping through to route detail, matching the other saved navigation transitions.",
            routeBookmarkSection.contains("indication = null"),
        )
    }
}
