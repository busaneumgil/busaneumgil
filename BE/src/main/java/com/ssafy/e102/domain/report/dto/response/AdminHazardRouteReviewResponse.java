package com.ssafy.e102.domain.report.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.ssafy.e102.domain.admin.dto.response.AdminRoutingApplyStatus;
import com.ssafy.e102.domain.report.entity.HazardReportRouteReview;
import com.ssafy.e102.domain.report.type.HazardRouteReviewIntent;
import com.ssafy.e102.domain.report.type.HazardRouteReviewStage;
import com.ssafy.e102.domain.report.type.ReportStatus;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient.GraphHopperReloadResult;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient.GraphHopperReloadStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "hazard route review response")
public record AdminHazardRouteReviewResponse(
	@Schema(description = "review ID", example = "1")
	Long reviewId,
	@Schema(description = "report ID", example = "10")
	Long reportId,
	@Schema(description = "review intent", example = "APPROVE")
	HazardRouteReviewIntent intent,
	@Schema(description = "review stage", example = "IN_PROGRESS")
	HazardRouteReviewStage stage,
	@Schema(description = "current report status", example = "PENDING")
	ReportStatus reportStatus,
	@Schema(description = "reviewer user ID")
	UUID reviewerUserId,
	@Schema(description = "review gu")
	String gu,
	@Schema(description = "review dong")
	String dong,
	@Schema(description = "selected segment edge ID", example = "41231")
	Long selectedSegmentEdgeId,
	@Schema(description = "review started at")
	LocalDateTime startedAt,
	@Schema(description = "review updated at")
	LocalDateTime updatedAt,
	@Schema(description = "review completed at")
	LocalDateTime completedAt,
	@Schema(description = "saved segment drafts")
	List<AdminHazardRouteReviewSegmentDraftResponse> segmentDrafts,
	@Schema(description = "routing apply status", example = "APPLIED")
	AdminRoutingApplyStatus routingApplyStatus,
	@Schema(description = "routing apply message")
	String routingApplyMessage) {

	public AdminHazardRouteReviewResponse(
		Long reviewId,
		Long reportId,
		HazardRouteReviewIntent intent,
		HazardRouteReviewStage stage,
		ReportStatus reportStatus,
		UUID reviewerUserId,
		String gu,
		String dong,
		Long selectedSegmentEdgeId,
		LocalDateTime startedAt,
		LocalDateTime updatedAt,
		LocalDateTime completedAt,
		List<AdminHazardRouteReviewSegmentDraftResponse> segmentDrafts) {
		this(
			reviewId,
			reportId,
			intent,
			stage,
			reportStatus,
			reviewerUserId,
			gu,
			dong,
			selectedSegmentEdgeId,
			startedAt,
			updatedAt,
			completedAt,
			segmentDrafts,
			AdminRoutingApplyStatus.SKIPPED,
			null);
	}

	public static AdminHazardRouteReviewResponse from(HazardReportRouteReview review, ReportStatus reportStatus) {
		return from(review, reportStatus, null);
	}

	public static AdminHazardRouteReviewResponse from(
		HazardReportRouteReview review,
		ReportStatus reportStatus,
		GraphHopperReloadResult routingApplyResult) {
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
				.toList(),
			toAdminRoutingApplyStatus(routingApplyResult),
			routingApplyResult == null ? null : routingApplyResult.message());
	}

	private static AdminRoutingApplyStatus toAdminRoutingApplyStatus(GraphHopperReloadResult routingApplyResult) {
		if (routingApplyResult == null) {
			return AdminRoutingApplyStatus.SKIPPED;
		}
		GraphHopperReloadStatus status = routingApplyResult.status();
		return switch (status) {
			case SKIPPED -> AdminRoutingApplyStatus.SKIPPED;
			case APPLIED -> AdminRoutingApplyStatus.APPLIED;
			case APPLIED_WITH_WARNING -> AdminRoutingApplyStatus.APPLIED_WITH_WARNING;
			case FAILED -> AdminRoutingApplyStatus.FAILED;
		};
	}
}
