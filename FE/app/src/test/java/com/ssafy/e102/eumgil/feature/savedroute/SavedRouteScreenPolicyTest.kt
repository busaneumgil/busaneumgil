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
}
