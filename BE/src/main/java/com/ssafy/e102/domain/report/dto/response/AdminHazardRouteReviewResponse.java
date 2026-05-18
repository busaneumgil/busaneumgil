package com.ssafy.e102.domain.report.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.ssafy.e102.domain.report.entity.HazardReportRouteReview;
import com.ssafy.e102.domain.report.type.HazardRouteReviewIntent;
import com.ssafy.e102.domain.report.type.HazardRouteReviewStage;
import com.ssafy.e102.domain.report.type.ReportStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "제보 경로 검수 응답")
public record AdminHazardRouteReviewResponse(
	@Schema(description = "검수 ID", example = "1")
	Long reviewId,
	@Schema(description = "제보 ID", example = "10")
	Long reportId,
	@Schema(description = "검수 유형", example = "APPROVE")
	HazardRouteReviewIntent intent,
	@Schema(description = "검수 진행 상태", example = "IN_PROGRESS")
	HazardRouteReviewStage stage,
	@Schema(description = "현재 제보 최종 상태", example = "PENDING")
	ReportStatus reportStatus,
	@Schema(description = "검수 관리자 사용자 ID")
	UUID reviewerUserId,
	@Schema(description = "검수 담당 구", example = "부산진구")
	String gu,
	@Schema(description = "검수 담당 동", example = "부전동")
	String dong,
	@Schema(description = "현재 선택 세그먼트 edge ID", example = "41231")
	Long selectedSegmentEdgeId,
	@Schema(description = "검수 시작 시각")
	LocalDateTime startedAt,
	@Schema(description = "마지막 수정 시각")
	LocalDateTime updatedAt,
	@Schema(description = "검수 완료 시각")
	LocalDateTime completedAt,
	@Schema(description = "저장된 세그먼트 draft 목록")
	List<AdminHazardRouteReviewSegmentDraftResponse> segmentDrafts) {

	public static AdminHazardRouteReviewResponse from(HazardReportRouteReview review, ReportStatus reportStatus) {
		if (review == null) {
			return null;
		}
		return new AdminHazardRouteReviewResponse(
			review.getReviewId(),
			review.getHazardReport().getReportId(),
			review.getIntent(),
			review.getStage(),
			reportStatus,
			review.getReviewerUserId(),
			review.getGu(),
			review.getDong(),
			review.getSelectedSegmentEdgeId(),
			review.getStartedAt(),
			review.getUpdatedAt(),
			review.getCompletedAt(),
			review.getSegmentDrafts().stream()
				.map(AdminHazardRouteReviewSegmentDraftResponse::from)
				.toList());
	}
}
