package com.ssafy.e102.domain.report.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.admin.service.AdminAuditLogService;
import com.ssafy.e102.domain.report.dto.response.AdminHazardReportDetailResponse;
import com.ssafy.e102.domain.report.dto.response.AdminHazardReportListResponse;
import com.ssafy.e102.domain.report.dto.response.AdminHazardReportStatusResponse;
import com.ssafy.e102.domain.report.entity.HazardReport;
import com.ssafy.e102.domain.report.entity.HazardReportImage;
import com.ssafy.e102.domain.report.exception.HazardReportErrorCode;
import com.ssafy.e102.domain.report.exception.HazardReportException;
import com.ssafy.e102.domain.report.repository.HazardReportImageRepository;
import com.ssafy.e102.domain.report.repository.HazardReportRepository;
import com.ssafy.e102.domain.report.type.ReportStatus;
import com.ssafy.e102.global.geo.GeoPointConverter;

@Service
@Transactional(readOnly = true)
public class AdminHazardReportService {

	private static final short REPRESENTATIVE_IMAGE_ORDER = 0;
	private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "reportId");

	private final HazardReportRepository hazardReportRepository;
	private final HazardReportImageRepository hazardReportImageRepository;
	private final GeoPointConverter geoPointConverter;
	private final AdminAuditLogService adminAuditLogService;

	public AdminHazardReportService(
		HazardReportRepository hazardReportRepository,
		HazardReportImageRepository hazardReportImageRepository,
		GeoPointConverter geoPointConverter,
		AdminAuditLogService adminAuditLogService) {
		this.hazardReportRepository = hazardReportRepository;
		this.hazardReportImageRepository = hazardReportImageRepository;
		this.geoPointConverter = geoPointConverter;
		this.adminAuditLogService = adminAuditLogService;
	}

	public AdminHazardReportListResponse getHazardReports(
		ReportStatus status,
		Long cursor,
		int size) {
		PageRequest pageRequest = PageRequest.of(0, size, NEWEST_FIRST);
		Slice<HazardReport> hazardReports = findHazardReports(status, cursor, pageRequest);
		return AdminHazardReportListResponse.of(
			hazardReports.getContent(),
			size,
			hazardReports.hasNext(),
			getRepresentativeImageUrls(hazardReports.getContent()),
			geoPointConverter);
	}

	public AdminHazardReportDetailResponse getHazardReportDetail(Long reportId) {
		HazardReport hazardReport = getHazardReport(reportId);
		return AdminHazardReportDetailResponse.of(hazardReport, geoPointConverter);
	}

	@Transactional
	public AdminHazardReportStatusResponse approveHazardReport(UUID actorUserId, Long reportId) {
		return updateHazardReportStatus(actorUserId, reportId, ReportStatus.APPROVED);
	}

	@Transactional
	public AdminHazardReportStatusResponse rejectHazardReport(UUID actorUserId, Long reportId) {
		return updateHazardReportStatus(actorUserId, reportId, ReportStatus.REJECTED);
	}

	private Slice<HazardReport> findHazardReports(ReportStatus status, Long cursor, PageRequest pageRequest) {
		if (status == null && cursor == null) {
			return hazardReportRepository.findAllForAdmin(pageRequest);
		}
		if (status == null) {
			return hazardReportRepository.findAllByReportIdLessThanForAdmin(cursor, pageRequest);
		}
		if (cursor == null) {
			return hazardReportRepository.findAllByStatusForAdmin(status, pageRequest);
		}
		return hazardReportRepository.findAllByStatusAndReportIdLessThanForAdmin(status, cursor, pageRequest);
	}

	private Map<Long, String> getRepresentativeImageUrls(List<HazardReport> hazardReports) {
		List<Long> reportIds = hazardReports.stream()
			.map(HazardReport::getReportId)
			.toList();
		if (reportIds.isEmpty()) {
			return Map.of();
		}
		return hazardReportImageRepository
			.findAllByHazardReport_ReportIdInAndDisplayOrder(reportIds, REPRESENTATIVE_IMAGE_ORDER)
			.stream()
			.collect(Collectors.toMap(
				image -> image.getHazardReport().getReportId(),
				HazardReportImage::getImageUrl,
				(existing, ignored) -> existing));
	}

	private HazardReport getHazardReport(Long reportId) {
		return hazardReportRepository.findWithImagesAndUserByReportId(reportId)
			.orElseThrow(() -> new HazardReportException(HazardReportErrorCode.HAZARD_REPORT_NOT_FOUND));
	}

	private AdminHazardReportStatusResponse updateHazardReportStatus(
		UUID actorUserId,
		Long reportId,
		ReportStatus nextStatus) {
		int updatedCount = hazardReportRepository.updateStatusIfCurrentStatus(
			reportId,
			ReportStatus.PENDING,
			nextStatus);
		if (updatedCount == 0) {
			throw getStatusUpdateFailure(reportId);
		}
		AdminHazardReportStatusResponse response = new AdminHazardReportStatusResponse(reportId, nextStatus);
		adminAuditLogService.record(
			actorUserId,
			nextStatus == ReportStatus.APPROVED ? "HAZARD_REPORT_APPROVE" : "HAZARD_REPORT_REJECT",
			"HAZARD_REPORT",
			String.valueOf(reportId),
			null,
			null,
			"도로 상태 제보를 " + nextStatus + " 상태로 변경 reportId=" + reportId,
			new AdminHazardReportStatusResponse(reportId, ReportStatus.PENDING),
			response);
		return response;
	}

	private HazardReportException getStatusUpdateFailure(Long reportId) {
		if (!hazardReportRepository.existsById(reportId)) {
			return new HazardReportException(HazardReportErrorCode.HAZARD_REPORT_NOT_FOUND);
		}
		return new HazardReportException(HazardReportErrorCode.HAZARD_REPORT_ALREADY_PROCESSED);
	}
}
