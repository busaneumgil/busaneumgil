package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
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
            listOf("\uD604\uC7AC \uC704\uCE58", "\uC548\uB0B4 \uC885\uB8CC"),
            lowVisionNavigationActionCards().map(LowVisionNavigationActionCard::label),
        )
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
    fun `navigation card content is sized to avoid clipping current location label`() {
        assertEquals(74.dp, LowVisionNavigationLayoutDefaults.currentLocationIconSize)
        assertEquals(44.sp, LowVisionNavigationLayoutDefaults.currentLocationLabelFontSize)
        assertEquals(12.dp, LowVisionNavigationLayoutDefaults.currentLocationVerticalPadding)
        assertEquals(76.sp, LowVisionNavigationLayoutDefaults.metricNumberFontSize)
        assertEquals(34.sp, LowVisionNavigationLayoutDefaults.metricUnitFontSize)
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
}
