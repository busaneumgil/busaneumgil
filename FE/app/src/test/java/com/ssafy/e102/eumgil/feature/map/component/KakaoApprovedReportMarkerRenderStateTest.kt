package com.ssafy.e102.eumgil.feature.map.component

import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class KakaoApprovedReportMarkerRenderStateTest {
    @Test
    fun `approved report overlay uses report type icon mapping`() {
        val marker =
            createKakaoOverlayMarkerRenderStates(
                overlayPoints =
                    listOf(
                        MapViewportPointOverlay(
                            overlayId = "approved-report:1",
                            coordinate = MapCoordinate(latitude = 35.1796, longitude = 129.0756),
                            kind = MapViewportPointKind.APPROVED_REPORT,
                            tone = MapViewportOverlayTone.ERROR,
                            reportTypeApiValue = "RAMP",
                            clickTargetId = "approved-report:1",
                        ),
                    ),
            ).single()

        assertEquals(KakaoOverlayMarkerKind.APPROVED_REPORT, marker.kind)
        assertEquals(R.drawable.ic_report_ramp, marker.iconResId)
    }

    @Test
    fun `approved report overlay falls back to other icon on unknown report type`() {
        val marker =
            createKakaoOverlayMarkerRenderStates(
                overlayPoints =
                    listOf(
                        MapViewportPointOverlay(
                            overlayId = "approved-report:2",
                            coordinate = MapCoordinate(latitude = 35.1796, longitude = 129.0756),
                            kind = MapViewportPointKind.APPROVED_REPORT,
                            tone = MapViewportOverlayTone.ERROR,
                            reportTypeApiValue = "UNKNOWN_TYPE",
                            clickTargetId = "approved-report:2",
                        ),
                    ),
            ).single()

        assertEquals(R.drawable.ic_report_other, marker.iconResId)
    }
}
