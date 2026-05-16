package com.ssafy.e102.eumgil.app.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavHostPolicyTest {
    @Test
    fun `mobility kws pauses while search voice route is active`() {
        assertTrue(
            shouldPauseMapKws(
                currentRoute = SearchRoute.VoiceInput.route,
                shouldPauseForMapVoiceInput = false,
            ),
        )
    }

    @Test
    fun `mobility kws pauses while map voice sheet is visible`() {
        assertTrue(
            shouldPauseMapKws(
                currentRoute = TopLevelRoute.Map.route,
                shouldPauseForMapVoiceInput = true,
            ),
        )
    }

    @Test
    fun `mobility kws resumes on regular map home when no voice ui is active`() {
        assertFalse(
            shouldPauseMapKws(
                currentRoute = TopLevelRoute.Map.route,
                shouldPauseForMapVoiceInput = false,
            ),
        )
    }
}
