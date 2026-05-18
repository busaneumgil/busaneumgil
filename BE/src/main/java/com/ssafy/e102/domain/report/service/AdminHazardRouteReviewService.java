package com.ssafy.e102.domain.report.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.admin.service.AdminAuditLogService;
import com.ssafy.e102.domain.admin.service.AdminService;
import com.ssafy.e102.domain.admin.type.AdminAreaAssignmentType;
import com.ssafy.e102.domain.report.dto.request.AdminHazardRouteReviewSegmentDraftRequest;
import com.ssafy.e102.domain.report.dto.request.StartHazardRouteReviewRequest;
import com.ssafy.e102.domain.report.dto.request.UpdateHazardRouteReviewRequest;
import com.ssafy.e102.domain.report.dto.response.AdminHazardRouteReviewResponse;
import com.ssafy.e102.domain.report.entity.HazardReport;
import com.ssafy.e102.domain.report.entity.HazardReportRouteReview;
import com.ssafy.e102.domain.report.entity.HazardReportRouteReviewSegmentDraft;
import com.ssafy.e102.domain.report.exception.HazardReportErrorCode;
import com.ssafy.e102.domain.report.exception.HazardReportException;
import com.ssafy.e102.domain.report.repository.HazardReportRepository;
import com.ssafy.e102.domain.report.repository.HazardReportRouteReviewRepository;
import com.ssafy.e102.domain.report.type.HazardRouteReviewIntent;
import com.ssafy.e102.domain.report.type.HazardRouteReviewStage;
import com.ssafy.e102.domain.report.type.ReportStatus;
import com.ssafy.e102.domain.route.repository.RoadSegmentRepository;

@Service
@Transactional(readOnly = true)
public class AdminHazardRouteReviewService {

	private final HazardReportRepository hazardReportRepository;
	private final HazardReportRouteReviewRepository hazardReportRouteReviewRepository;
	private final RoadSegmentRepository roadSegmentRepository;
	private final AdminService adminService;
	private final AdminAuditLogService adminAuditLogService;
	private final Clock clock;

	@Autowired
	public AdminHazardRouteReviewService(
		HazardReportRepository hazardReportRepository,
		HazardReportRouteReviewRepository hazardReportRouteReviewRepository,
		RoadSegmentRepository roadSegmentRepository,
		AdminService adminService,
		AdminAuditLogService adminAuditLogService,
		Clock clock) {
		this.hazardReportRepository = hazardReportRepository;
		this.hazardReportRouteReviewRepository = hazardReportRouteReviewRepository;
		this.roadSegmentRepository = roadSegmentRepository;
		this.adminService = adminService;
		this.adminAuditLogService = adminAuditLogService;
		this.clock = clock;
	}

	@Transactional
	public AdminHazardRouteReviewResponse startRouteReview(
		UUID userId,
		Long reportId,
		StartHazardRouteReviewRequest request) {
		HazardReport hazardReport = getHazardReport(reportId);
		adminService.requireCanEditArea(userId, request.gu(), request.dong(), AdminAreaAssignmentType.ROAD_NETWORK);
		HazardReportRouteReview latestReview = findLatestReview(reportId);
		validateStartReview(hazardReport, latestReview, request.intent());

		if (latestReview != null && latestReview.isInProgress() && latestReview.getIntent() == request.intent()) {
			latestReview.validateOwnedBy(userId);
			return AdminHazardRouteReviewResponse.from(latestReview, hazardReport.getStatus());
		}

		LocalDateTime now = LocalDateTime.now(clock);
		HazardReportRouteReview review = HazardReportRouteReview.start(
			hazardReport,
			request.intent(),
			userId,
			request.gu(),
			request.dong(),
			now);
		HazardReportRouteReview savedReview = saveRouteReview(review);
		adminAuditLogService.record(
			userId,
			"HAZARD_REPORT_ROUTE_REVIEW_START",
			"HAZARD_REPORT",
			String.valueOf(reportId),
			request.gu(),
			request.dong(),
			"제보 경로 검수 시작 reportId=" + reportId + " intent=" + request.intent(),
			null,
			AdminHazardRouteReviewResponse.from(savedReview, hazardReport.getStatus()));
		return AdminHazardRouteReviewResponse.from(savedReview, hazardReport.getStatus());
	}

	@Transactional
	public AdminHazardRouteReviewResponse updateRouteReview(
		UUID userId,
		Long reportId,
		UpdateHazardRouteReviewRequest request) {
		HazardReportRouteReview review = getInProgressReview(reportId);
		review.validateOwnedBy(userId);
		AdminHazardRouteReviewResponse before = AdminHazardRouteReviewResponse.from(review, review.getHazardReport().getStatus());

		if (request.selectedSegmentEdgeId() != null) {
			validateEdgeInArea(request.selectedSegmentEdgeId(), review.getGu(), review.getDong());
			review.selectSegment(request.selectedSegmentEdgeId());
		}
		if (request.segmentDrafts() != null) {
			review.replaceSegmentDrafts(toSegmentDraftEntities(request.segmentDrafts(), review.getGu(), review.getDong()));
		}

		adminAuditLogService.record(
			userId,
			"HAZARD_REPORT_ROUTE_REVIEW_UPDATE",
			"HAZARD_REPORT",
			String.valueOf(reportId),
			review.getGu(),
			review.getDong(),
			"제보 경로 검수 draft 저장 reportId=" + reportId,
			before,
			AdminHazardRouteReviewResponse.from(review, review.getHazardReport().getStatus()));
		return AdminHazardRouteReviewResponse.from(review, review.getHazardReport().getStatus());
	}

	@Transactional
	public AdminHazardRouteReviewResponse completeRouteReview(UUID userId, Long reportId) {
		HazardReportRouteReview review = getInProgressReview(reportId);
		HazardReport hazardReport = review.getHazardReport();
		review.validateOwnedBy(userId);
		AdminHazardRouteReviewResponse before = AdminHazardRouteReviewResponse.from(review, hazardReport.getStatus());
		LocalDateTime now = LocalDateTime.now(clock);

		if (review.getIntent() == HazardRouteReviewIntent.APPROVE) {
			hazardReport.approve(userId, now);
		} else {
			validateRestorable(hazardReport, review);
			hazardReport.markProcessed(userId, now);
		}
		review.complete(now);

		adminAuditLogService.record(
			userId,
			"HAZARD_REPORT_ROUTE_REVIEW_COMPLETE",
			"HAZARD_REPORT",
			String.valueOf(reportId),
			review.getGu(),
			review.getDong(),
			"제보 경로 검수 완료 reportId=" + reportId + " intent=" + review.getIntent(),
			before,
			AdminHazardRouteReviewResponse.from(review, hazardReport.getStatus()));
		return AdminHazardRouteReviewResponse.from(review, hazardReport.getStatus());
	}

	public AdminHazardRouteReviewResponse getLatestRouteReview(Long reportId, ReportStatus reportStatus) {
		return AdminHazardRouteReviewResponse.from(findLatestReview(reportId), reportStatus);
	}

	private HazardReport getHazardReport(Long reportId) {
		return hazardReportRepository.findWithImagesAndUserByReportId(reportId)
			.orElseThrow(() -> new HazardReportException(HazardReportErrorCode.HAZARD_REPORT_NOT_FOUND));
	}

	private HazardReportRouteReview saveRouteReview(HazardReportRouteReview review) {
		try {
			return hazardReportRouteReviewRepository.save(review);
		} catch (DataIntegrityViolationException exception) {
			throw new HazardReportException(
				HazardReportErrorCode.HAZARD_ROUTE_REVIEW_CONFLICT,
				"이미 진행 중인 제보 경로 검수가 있어 새 검수를 시작할 수 없습니다.");
		}
	}

	private HazardReportRouteReview findLatestReview(Long reportId) {
		return hazardReportRouteReviewRepository.findTopByHazardReport_ReportIdOrderByReviewIdDesc(reportId)
			.orElse(null);
	}

	private HazardReportRouteReview getInProgressReview(Long reportId) {
		return hazardReportRouteReviewRepository
			.findTopByHazardReport_ReportIdAndStageOrderByReviewIdDesc(reportId, HazardRouteReviewStage.IN_PROGRESS)
			.orElseThrow(() -> new HazardReportException(
				HazardReportErrorCode.HAZARD_ROUTE_REVIEW_NOT_FOUND,
				"진행 중인 제보 경로 검수가 없습니다."));
	}

	private void validateStartReview(
		HazardReport hazardReport,
		HazardReportRouteReview latestReview,
		HazardRouteReviewIntent intent) {
		if (intent == HazardRouteReviewIntent.APPROVE) {
			validateApprovable(hazardReport, latestReview);
			return;
		}
		validateRestorable(hazardReport, latestReview);
	}

	private void validateApprovable(HazardReport hazardReport, HazardReportRouteReview latestReview) {
		if (latestReview != null && latestReview.isInProgress() && latestReview.getIntent() != HazardRouteReviewIntent.APPROVE) {
			throw new HazardReportException(
				HazardReportErrorCode.HAZARD_ROUTE_REVIEW_CONFLICT,
				"이미 다른 검수가 진행 중입니다.");
		}
		if (hazardReport.getStatus() != ReportStatus.PENDING && hazardReport.getStatus() != ReportStatus.REJECTED) {
			throw new HazardReportException(
				HazardReportErrorCode.HAZARD_ROUTE_REVIEW_CONFLICT,
				"현재 상태에서는 승인 검수를 시작할 수 없습니다.");
		}
	}

	private void validateRestorable(HazardReport hazardReport, HazardReportRouteReview latestReview) {
		if (latestReview != null && latestReview.isInProgress() && latestReview.getIntent() != HazardRouteReviewIntent.RESTORE) {
			throw new HazardReportException(
				HazardReportErrorCode.HAZARD_ROUTE_REVIEW_CONFLICT,
				"이미 다른 검수가 진행 중입니다.");
		}
		if (latestReview != null && latestReview.isCompleted() && latestReview.getIntent() == HazardRouteReviewIntent.RESTORE) {
			throw new HazardReportException(
				HazardReportErrorCode.HAZARD_ROUTE_REVIEW_CONFLICT,
				"이미 원상복구 검수가 완료된 제보입니다.");
		}
		if (hazardReport.getStatus() != ReportStatus.APPROVED) {
			throw new HazardReportException(
				HazardReportErrorCode.HAZARD_ROUTE_REVIEW_CONFLICT,
				"승인 완료된 제보만 원상복구 검수를 시작할 수 있습니다.");
		}
	}

	private List<HazardReportRouteReviewSegmentDraft> toSegmentDraftEntities(
		List<AdminHazardRouteReviewSegmentDraftRequest> requests,
		String gu,
		String dong) {
		return requests.stream()
			.map(request -> {
				validateEdgeInArea(request.edgeId(), gu, dong);
				return HazardReportRouteReviewSegmentDraft.create(
					request.edgeId(),
					request.walkAccess(),
					request.brailleBlockState(),
					request.audioSignalState(),
					request.widthState(),
					request.surfaceState(),
					request.stairsState(),
					request.signalState());
			})
			.toList();
	}

	private void validateEdgeInArea(Long edgeId, String gu, String dong) {
		if (!roadSegmentRepository.existsIntersectingAreaByEdgeId(edgeId, gu, dong)) {
			throw new HazardReportException(
				HazardReportErrorCode.INVALID_HAZARD_ROUTE_REVIEW_REQUEST,
				"검수 범위에 속한 세그먼트만 저장할 수 있습니다.");
		}
	}
}
