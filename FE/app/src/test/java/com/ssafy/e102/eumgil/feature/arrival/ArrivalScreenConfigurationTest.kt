package com.ssafy.e102.eumgil.feature.arrival

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrivalScreenConfigurationTest {
    @Test
    fun `arrival completion content uses dedicated background band asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/arrival/ArrivalScreen.kt").readText()

        assertTrue(
            "Arrival completion content should use a dedicated arrival illustration asset instead of reusing the splash background.",
            source.contains("R.drawable.arrival_completion_background"),
        )
        assertFalse(
            "Arrival completion content should no longer reuse the splash illustration drawable because it is also used by the app launch surfaces.",
            source.contains("R.drawable.splash_illustration"),
        )
    }

    @Test
    fun `arrival completion headline keeps the approved centered line break`() {
        val stringsSource =
            File("src/main/res/values/strings.xml").readText()

        assertTrue(
            "Arrival headline should keep the centered two-line copy requested for the completion screen.",
            stringsSource.contains("<string name=\"arrival_screen_headline\">오늘도 안전한 이동\\n수고하셨습니다!</string>"),
        )
    }

    @Test
    fun `arrival completion content promotes the illustration to a top background band`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/arrival/ArrivalScreen.kt").readText()

        assertTrue(
            "Arrival completion content should define a dedicated hero-band height so the illustration reads like a background section instead of a card.",
            source.contains("private val ArrivalHeroBandHeight = 236.dp"),
        )
        assertTrue(
            "Arrival completion content should crop the illustration to fill the hero band width.",
            source.contains("contentScale = ContentScale.Crop"),
        )
        assertTrue(
            "Arrival completion content should anchor the illustration to the bottom of the hero band to preserve the skyline composition from the approved design.",
            source.contains(".align(Alignment.BottomCenter)"),
        )
        assertTrue(
            "Arrival completion content should leave the hero band full-bleed by moving horizontal padding into inner content blocks.",
            source.contains("private val ArrivalHeroBandTopSpacing = 20.dp"),
        )
        assertTrue(
            "Arrival completion content should push the hero band slightly lower from the top edge to match the approved composition.",
            source.contains("Spacer(modifier = Modifier.height(ArrivalHeroBandTopSpacing))"),
        )
        assertFalse(
            "Arrival completion content should not keep the old root padding that inset the hero band on both sides.",
            source.contains(".padding(horizontal = EumSpacing.large, vertical = EumSpacing.medium)"),
        )
    }

    @Test
    fun `arrival evaluation sheet uses edge to edge bottom sheet placement`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/arrival/ArrivalScreen.kt").readText()

        assertFalse(
            "Arrival evaluation sheet should not cap the sheet width when it needs to span the full bottom edge.",
            source.contains("Modifier.widthIn(max = 520.dp)"),
        )
        assertFalse(
            "Arrival evaluation sheet should not keep the old bottom-sheet wrapper that added navigation-bar and bottom spacing outside the sheet container.",
            source.contains(
                ".navigationBarsPadding()\n                    .padding(horizontal = EumSpacing.medium)\n                    .padding(bottom = EumSpacing.medium)",
            ),
        )
    }

    @Test
    fun `arrival evaluation sheet supports drag dismiss and yellow selected stars`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/arrival/ArrivalScreen.kt").readText()

        assertTrue(
            "Arrival evaluation sheet should expose a draggable handle so users can pull the sheet down to dismiss it.",
            source.contains("handleModifier ="),
        )
        assertTrue(
            "Arrival evaluation sheet should wire vertical dragging into the handle dismiss interaction.",
            source.contains(".draggable("),
        )
        assertTrue(
            "Selected arrival rating stars should use the yellow rating tint requested for the evaluation flow.",
            source.contains("private val ArrivalRatingSelectedColor = Color(0xFFFACC15)"),
        )
    }

    @Test
    fun `arrival evaluation sheet tightens rating action spacing and removes route save dialog`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/arrival/ArrivalScreen.kt").readText()

        assertTrue(
            "Arrival evaluation sheet should define a tighter placeholder gap between the star row and the action buttons.",
            source.contains("private val ArrivalEvaluationRatingFeedbackPlaceholderHeight = 8.dp"),
        )
        assertFalse(
            "Arrival screen should no longer render the route save dialog flow after the save action becomes immediate.",
            source.contains("if (uiState.isRouteSaveDialogVisible)"),
        )
        assertFalse(
            "Arrival screen should no longer keep the route save dialog composable in this evaluation flow.",
            source.contains("private fun ArrivalRouteSaveDialog("),
        )
    }

    @Test
    fun `arrival explore new route navigation removes completion screen from back stack`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/app/navigation/MainNavGraph.kt").readText()

        assertTrue(
            "Exploring a new route from the arrival screen should navigate to search.",
            source.contains("navController.navigate(SearchRoute.Entry.createRoute())"),
        )
        assertTrue(
            "Exploring a new route from the arrival screen should remove the completion screen from the back stack so back does not reopen the evaluation flow.",
            source.contains("popUpTo(ArrivalRoute.Entry.route) {\n                        inclusive = true"),
        )
    }
}
