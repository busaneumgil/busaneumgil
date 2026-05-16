package com.ssafy.e102.eumgil.app.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavHostPolicyTest {
    @Test
    fun `global voice assistant requests map facility cleanup only on active map route`() {
        assertTrue(shouldRequestMapFacilityDetailDismissOnGlobalVoiceAssistantOpen(TopLevelRoute.Map.route))
        assertFalse(shouldRequestMapFacilityDetailDismissOnGlobalVoiceAssistantOpen(SearchRoute.Entry.route))
        assertFalse(shouldRequestMapFacilityDetailDismissOnGlobalVoiceAssistantOpen(null))
    }

    @Test
    fun `opening global voice assistant from map writes a saved state dismiss request`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/app/navigation/AppNavHost.kt")
                .readText()
        val showVoiceAssistantSource =
            source
                .substringAfter("fun showVoiceAssistant(sourceContext: VoiceAssistantContext) {")
                .substringBefore("val voiceAssistantPermissionLauncher")

        assertTrue(
            "showVoiceAssistant should request a one-shot map cleanup event before showing the global overlay on the map route.",
            showVoiceAssistantSource.contains("shouldRequestMapFacilityDetailDismissOnGlobalVoiceAssistantOpen(currentRoute)") &&
                showVoiceAssistantSource.contains("requestMapFacilityDetailDismiss()"),
        )
    }

    @Test
    fun `mobility kws pauses while global voice assistant overlay is visible`() {
        assertTrue(
            shouldPauseMapKws(
                currentRoute = TopLevelRoute.Map.route,
                shouldPauseForMapVoiceInput = false,
                voiceAssistantVisible = true,
            ),
        )
    }

    @Test
    fun `mobility kws keeps search voice route pause for route compatibility`() {
        assertTrue(
            shouldPauseMapKws(
                currentRoute = SearchRoute.VoiceInput.route,
                shouldPauseForMapVoiceInput = false,
                voiceAssistantVisible = false,
            ),
        )
    }

    @Test
    fun `mobility kws pauses while map voice sheet is visible`() {
        assertTrue(
            shouldPauseMapKws(
                currentRoute = TopLevelRoute.Map.route,
                shouldPauseForMapVoiceInput = true,
                voiceAssistantVisible = false,
            ),
        )
    }

    @Test
    fun `mobility kws resumes on regular map home when overlay is hidden and no voice ui is active`() {
        assertFalse(
            shouldPauseMapKws(
                currentRoute = TopLevelRoute.Map.route,
                shouldPauseForMapVoiceInput = false,
                voiceAssistantVisible = false,
            ),
        )
    }

    @Test
    fun `mobility kws does not pause only because navigation guidance route is active`() {
        assertFalse(
            shouldPauseMapKws(
                currentRoute = NavigationRoute.Guidance.route,
                shouldPauseForMapVoiceInput = false,
                voiceAssistantVisible = false,
            ),
        )
    }
}
