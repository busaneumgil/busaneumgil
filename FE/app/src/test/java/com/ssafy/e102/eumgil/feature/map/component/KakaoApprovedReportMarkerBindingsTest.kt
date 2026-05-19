package com.ssafy.e102.eumgil.feature.map.component

import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KakaoApprovedReportMarkerBindingsTest {
    @Test
    fun `approved report point converts to kakao approved report marker`() {
        val point =
            MapViewportPointOverlay(
                overlayId = "approved-report:42",
                coordinate = MapCoordinate(35.1796, 129.0756),
                kind = MapViewportPointKind.APPROVED_REPORT,
                reportTypeApiValue = "RAMP",
                label = "경사로 문제",
                includeInProjection = false,
                clickTargetId = "approved-report:42",
            )

        val marker = point.toKakaoProjectedPointMarkerState()

        assertEquals(KakaoOverlayMarkerKind.APPROVED_REPORT, marker?.kind)
        assertEquals(32, marker?.sizeDp)
        assertEquals(R.drawable.ic_report_ramp, marker?.iconResId)
        assertEquals("approved-report:42", marker?.clickTargetId)
    }

    @Test
    fun `approved report point falls back to other icon when report type is unknown`() {
        val point =
            MapViewportPointOverlay(
                overlayId = "approved-report:99",
                coordinate = MapCoordinate(35.1796, 129.0756),
                kind = MapViewportPointKind.APPROVED_REPORT,
                reportTypeApiValue = "UNKNOWN_TYPE",
                includeInProjection = false,
                clickTargetId = "approved-report:99",
            )

        val marker = point.toKakaoProjectedPointMarkerState()

        assertEquals(R.drawable.ic_report_other, marker?.iconResId)
    }

    @Test
    fun `approved report kakao layer stays above facility marker layer`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/KakaoMapViewport.kt")
                .readText()

        assertTrue(source.contains("KAKAO_APPROVED_REPORT_MARKER_LAYER_ID"))
        assertTrue(source.contains("KAKAO_APPROVED_REPORT_MARKER_LAYER_Z_ORDER = 1005"))
        assertTrue(source.contains("KAKAO_MARKER_LAYER_Z_ORDER = 1000"))
    }
}
