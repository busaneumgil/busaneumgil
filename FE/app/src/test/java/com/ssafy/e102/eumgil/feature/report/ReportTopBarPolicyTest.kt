package com.ssafy.e102.eumgil.feature.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTopBarPolicyTest {
    @Test
    fun `report root step hides back button`() {
        assertFalse(reportTopBarShowsBackButton(ReportStep.TypeSelection))
    }

    @Test
    fun `report intermediate steps keep back button`() {
        assertTrue(reportTopBarShowsBackButton(ReportStep.LocationConfirm))
        assertTrue(reportTopBarShowsBackButton(ReportStep.DetailInput))
    }

    @Test
    fun `report completion hides back button`() {
        assertFalse(reportTopBarShowsBackButton(ReportStep.Complete))
    }

    // ─── Task 1.3 — 단계별 TopBar에 선택 type 라벨 표시 ─────────────────────
    //
    // 설계 근거: LocationConfirm / DetailInput 단계는 화면 콘텐츠(지도 / 입력 필드)로
    // 어떤 단계인지 충분히 인지 가능하므로, 단계명 대신 사용자가 선택한 type 라벨만
    // 노출하여 "지금 어떤 유형의 제보를 작성 중인지"를 시각 위계의 최상위로 둔다.

    @Test
    fun `type selection step always shows base label regardless of selected type`() {
        assertEquals("제보", reportStepTitle(ReportStep.TypeSelection))
        assertEquals("제보", reportStepTitle(ReportStep.TypeSelection, ReportType.STAIRS_STEP))
    }

    @Test
    fun `location confirm step shows only type label when type is selected`() {
        assertEquals(
            "계단·단차 있음",
            reportStepTitle(ReportStep.LocationConfirm, ReportType.STAIRS_STEP),
        )
        assertEquals(
            "점자블록 문제",
            reportStepTitle(ReportStep.LocationConfirm, ReportType.BRAILLE_BLOCK),
        )
        assertEquals(
            "경사로 문제",
            reportStepTitle(ReportStep.LocationConfirm, ReportType.RAMP),
        )
    }

    @Test
    fun `detail input step shows only type label when type is selected`() {
        assertEquals(
            "계단·단차 있음",
            reportStepTitle(ReportStep.DetailInput, ReportType.STAIRS_STEP),
        )
        assertEquals(
            "인도 없음",
            reportStepTitle(ReportStep.DetailInput, ReportType.SIDEWALK_MISSING),
        )
        assertEquals(
            "기타 장애물",
            reportStepTitle(ReportStep.DetailInput, ReportType.OTHER_OBSTACLE),
        )
    }

    @Test
    fun `intermediate steps fall back to step name when type is null`() {
        // type이 null인 경우는 비정상 흐름(필수값 위반)이지만 graceful fallback으로 단계명 노출.
        assertEquals("위치 확인", reportStepTitle(ReportStep.LocationConfirm, null))
        assertEquals("상세 정보 입력", reportStepTitle(ReportStep.DetailInput, null))
    }

    @Test
    fun `complete step keeps base label even with type selected because summary card already shows it`() {
        assertEquals("제보 완료", reportStepTitle(ReportStep.Complete))
        assertEquals(
            "제보 완료",
            reportStepTitle(ReportStep.Complete, ReportType.SIDEWALK_WIDTH),
        )
    }
}
