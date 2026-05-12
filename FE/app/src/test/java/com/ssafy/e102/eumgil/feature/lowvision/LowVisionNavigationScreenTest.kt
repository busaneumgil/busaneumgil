package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.feature.navigation.NavigationScreenState
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiAction
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LowVisionNavigationScreenTest {
    @Test
    fun `navigation screen keeps distance and eta as top summary metrics`() {
        assertEquals(
            listOf("\uB0A8\uC740 \uAC70\uB9AC", "\uB0A8\uC740 \uC2DC\uAC04"),
            lowVisionNavigationMetricSections().map(LowVisionNavigationMetricSection::label),
        )
    }

    @Test
    fun `navigation screen exposes current location and exit action cards`() {
        assertEquals(
            listOf("\uD604\uC7AC \uC704\uCE58", "\uC548\uB0B4 \uC644\uB8CC"),
            lowVisionNavigationActionCards().map(LowVisionNavigationActionCard::label),
        )
    }

    @Test
    fun `navigation exit card completes navigation directly without hidden confirm dialog`() {
        assertEquals(NavigationUiAction.NavigationCompleteClicked, lowVisionNavigationExitAction())
    }

    @Test
    fun `navigation distance metric appends meters and converts long distances to kilometers`() {
        val section = LowVisionNavigationMetricSection(label = "\uB0A8\uC740 \uAC70\uB9AC", metricIndex = 0)

        assertEquals("98m", lowVisionNavigationDisplayMetric(section, "98"))
        assertEquals("980m", lowVisionNavigationDisplayMetric(section, "980m"))
        assertEquals("1.2km", lowVisionNavigationDisplayMetric(section, "1200"))
        assertEquals("1.5km", lowVisionNavigationDisplayMetric(section, "1500m"))
    }

    @Test
    fun `navigation time metric always displays minutes`() {
        val section = LowVisionNavigationMetricSection(label = "\uB0A8\uC740 \uC2DC\uAC04", metricIndex = 1)

        assertEquals("16\uBD84", lowVisionNavigationDisplayMetric(section, "16"))
        assertEquals("2\uBD84", lowVisionNavigationDisplayMetric(section, "2\uBD84"))
    }

    @Test
    fun `navigation metric talkback text reads label and formatted value as one phrase`() {
        val distanceSection = LowVisionNavigationMetricSection(label = "\uB0A8\uC740 \uAC70\uB9AC", metricIndex = 0)
        val timeSection = LowVisionNavigationMetricSection(label = "\uB0A8\uC740 \uC2DC\uAC04", metricIndex = 1)

        assertEquals(
            "\uB0A8\uC740 \uAC70\uB9AC 98m",
            distanceSection.talkBackText("98m"),
        )
        assertEquals(
            "\uB0A8\uC740 \uC2DC\uAC04 16\uBD84",
            timeSection.talkBackText("16\uBD84"),
        )
    }

    @Test
    fun `navigation current location card prefers resolved address over gps coordinates`() {
        val display =
            lowVisionCurrentLocationDisplay(
                latitude = 35.179612,
                longitude = 129.075634,
                address = "\uBD80\uC0B0\uAD11\uC5ED\uC2DC \uBD80\uC0B0\uC9C4\uAD6C \uC911\uC559\uB300\uB85C 100",
            )

        assertEquals("\uD604\uC7AC \uC704\uCE58", display.title)
        assertEquals("", display.supportingText)
        assertEquals(
            "\uD604\uC7AC \uC704\uCE58 \uBD80\uC0B0\uAD11\uC5ED\uC2DC \uBD80\uC0B0\uC9C4\uAD6C \uC911\uC559\uB300\uB85C 100",
            display.talkBackText,
        )
    }

    @Test
    fun `navigation current location card falls back to gps coordinates when address is blank`() {
        val display =
            lowVisionCurrentLocationDisplay(
                latitude = 35.179612,
                longitude = 129.075634,
                address = " ",
            )

        assertEquals("\uD604\uC7AC \uC704\uCE58", display.title)
        assertEquals("", display.supportingText)
        assertEquals(
            "\uD604\uC7AC \uC704\uCE58 \uC704\uB3C4 35.17961\uB3C4 \uACBD\uB3C4 129.07563\uB3C4",
            display.talkBackText,
        )
    }

    @Test
    fun `navigation card content is sized to avoid clipping current location label`() {
        assertEquals(74.dp, LowVisionNavigationLayoutDefaults.currentLocationIconSize)
        assertEquals(44.sp, LowVisionNavigationLayoutDefaults.currentLocationLabelFontSize)
        assertEquals(12.dp, LowVisionNavigationLayoutDefaults.currentLocationVerticalPadding)
        assertEquals(76.sp, LowVisionNavigationLayoutDefaults.metricNumberFontSize)
        assertEquals(34.sp, LowVisionNavigationLayoutDefaults.metricUnitFontSize)
        assertEquals(22.dp, LowVisionNavigationLayoutDefaults.exitCardVerticalPadding)
        assertEquals(92.dp, LowVisionNavigationLayoutDefaults.exitIconContainerSize)
        assertEquals(16.dp, LowVisionNavigationLayoutDefaults.exitIconTextGap)
        assertEquals(48.sp, LowVisionNavigationLayoutDefaults.exitLabelFontSize)
        assertEquals(54.sp, LowVisionNavigationLayoutDefaults.exitLabelLineHeight)
    }

    @Test
    fun `navigation screen keeps low vision bottom tab order`() {
        assertEquals(
            listOf(
                LowVisionBottomTab.HOME,
                LowVisionBottomTab.BOOKMARK,
                LowVisionBottomTab.CATEGORY,
                LowVisionBottomTab.MY_PAGE,
            ),
            lowVisionNavigationBottomTabs(),
        )
    }

    @Test
    fun `navigation screen shows load error only while route guidance is still loading`() {
        assertTrue(
            shouldShowLowVisionNavigationLoadError(
                uiState = NavigationUiState(screenState = NavigationScreenState.Loading),
                loadErrorMessage = LOW_VISION_NAVIGATION_LOAD_ERROR_MESSAGE,
            ),
        )
        assertFalse(
            shouldShowLowVisionNavigationLoadError(
                uiState = NavigationUiState(screenState = NavigationScreenState.Ready),
                loadErrorMessage = LOW_VISION_NAVIGATION_LOAD_ERROR_MESSAGE,
            ),
        )
        assertFalse(
            shouldShowLowVisionNavigationLoadError(
                uiState = NavigationUiState(screenState = NavigationScreenState.Loading),
                loadErrorMessage = null,
            ),
        )
    }

    @Test
    fun `navigation route wires low vision tts events and initial briefing trigger`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/lowvision/LowVisionNavigationRoute.kt")
                .readText()

        assertTrue(source.contains("AndroidTextToSpeechController"))
        assertTrue(source.contains("viewModel.updateTextToSpeechState"))
        assertTrue(source.contains("is NavigationUiEvent.SpeakBriefing -> textToSpeechController.speak(event.text)"))
        assertTrue(source.contains("NavigationUiEvent.StopBriefing -> textToSpeechController.stop()"))
        assertTrue(source.contains("NavigationUiAction.NavigationEntered"))
    }
}
