package com.ssafy.e102.eumgil.feature.savedroute

import java.io.File
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
            "Saved-route list action buttons should avoid forcing full-width content inside list rows.",
            !primaryActionSection.contains("fullWidthContent = true"),
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
}
