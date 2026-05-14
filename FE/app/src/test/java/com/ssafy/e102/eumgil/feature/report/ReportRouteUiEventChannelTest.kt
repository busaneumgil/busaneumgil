package com.ssafy.e102.eumgil.feature.report

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 1.1 — ReportRoute UI 이벤트 채널 일괄 연결 회귀 가드.
 *
 * `ReportViewModel`이 emit 중인 `AnnounceForAccessibility`(3곳) / `ScrollToFirstError`(1곳)
 * 이벤트가 다시 `Unit`으로 무시되는 상태로 회귀하지 않도록 막는다.
 */
class ReportRouteUiEventChannelTest {
    @Test
    fun `report route announces accessibility events through host view`() {
        val source = reportRouteSource()

        assertTrue(
            "ReportRoute should obtain LocalView so it can announce accessibility messages.",
            source.contains("val view = LocalView.current"),
        )
        assertTrue(
            "ReportRoute should announce AnnounceForAccessibility events via view.announceForAccessibility.",
            source.contains("view.announceForAccessibility(event.message)"),
        )
    }

    @Test
    fun `report route scrolls to top on ScrollToFirstError`() {
        val source = reportRouteSource()

        assertTrue(
            "ReportRoute should remember a ScrollState for hoisting to ReportScreen.",
            source.contains("val scrollState = rememberScrollState()"),
        )
        assertTrue(
            "ReportRoute should animate scroll to top when ScrollToFirstError is emitted.",
            source.contains("ReportUiEvent.ScrollToFirstError -> scrollState.animateScrollTo(0)"),
        )
    }

    @Test
    fun `report screen accepts scroll state from route`() {
        val source = reportScreenSource()

        assertTrue(
            "ReportScreen should accept a ScrollState so route can control scroll for ScrollToFirstError.",
            source.contains("scrollState: ScrollState"),
        )
        assertTrue(
            "ReportScreen should use the hoisted scrollState for the scrollable Column.",
            source.contains("Modifier.verticalScroll(scrollState)"),
        )
    }

    private fun reportRouteSource(): String =
        File("src/main/java/com/ssafy/e102/eumgil/feature/report/ReportRoute.kt").readText()

    private fun reportScreenSource(): String =
        File("src/main/java/com/ssafy/e102/eumgil/feature/report/ReportScreen.kt").readText()
}
