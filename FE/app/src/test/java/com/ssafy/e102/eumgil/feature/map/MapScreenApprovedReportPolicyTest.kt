package com.ssafy.e102.eumgil.feature.map

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MapScreenApprovedReportPolicyTest {
    private val source =
        File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt")
            .readText()

    @Test
    fun `approved report marker click is routed to the dedicated action`() {
        assertTrue(
            "MapScreen should include approved reports in the viewport overlay and parse their click target before falling back to facility marker taps.",
            source.contains("approvedReportMarkers = uiState.approvedReportMarkerState.visibleReports") &&
                source.contains("parseApprovedReportClickTargetId(clickTargetId)?.let { reportId ->") &&
                source.contains("MapUiAction.ApprovedReportMarkerTapped(reportId = reportId)") &&
                source.contains("MapUiAction.MarkerTapped(clickTargetId)"),
        )
    }

    @Test
    fun `approved report sheet is mutually exclusive with recent destination and facility sheets`() {
        assertTrue(
            "Recent destination sheet should be hidden while approved report sheet is visible.",
            Regex(
                "recentDestinationSheetState\\.isVisible\\s*&&\\s*" +
                    "uiState\\.approvedReportSheetState\\.isVisible\\.not\\(\\)\\s*&&\\s*" +
                    "facilityDetailSheetUiState\\.isVisible\\.not\\(\\)",
            ).containsMatchIn(source),
        )
        assertTrue(
            "Facility detail sheet should be hidden while approved report sheet is visible.",
            Regex(
                "facilityDetailSheetUiState\\.isVisible\\s*&&\\s*" +
                    "uiState\\.approvedReportSheetState\\.isVisible\\.not\\(\\)\\s*&&\\s*" +
                    "uiState\\.isVoiceSearchVisible\\.not\\(\\)",
            ).containsMatchIn(source),
        )
        assertTrue(
            "Approved report sheet should not be shown over route endpoint picker or voice search.",
            Regex(
                "ApprovedReportBottomSheetShell\\([\\s\\S]*" +
                    "if \\(uiState\\.approvedReportSheetState\\.isVisible\\s*&&\\s*" +
                    "uiState\\.routeEndpointMapPickerState == null\\s*&&\\s*" +
                    "uiState\\.isVoiceSearchVisible\\.not\\(\\)\\)[\\s\\S]*" +
                    "MapUiAction\\.ApprovedReportSheetDismissed",
            ).containsMatchIn(source),
        )
    }
}
