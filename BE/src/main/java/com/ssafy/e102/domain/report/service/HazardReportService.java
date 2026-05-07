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

import com.ssafy.e102.domain.report.dto.request.CreateHazardReportRequest;
import com.ssafy.e102.domain.report.dto.response.HazardReportDetailResponse;
import com.ssafy.e102.domain.report.dto.response.HazardReportIdResponse;
import com.ssafy.e102.domain.report.dto.response.HazardReportListResponse;
import com.ssafy.e102.domain.report.entity.HazardReport;
import com.ssafy.e102.domain.report.entity.HazardReportImage;
import com.ssafy.e102.domain.report.exception.HazardReportErrorCode;
import com.ssafy.e102.domain.report.exception.HazardReportException;
import com.ssafy.e102.domain.report.repository.HazardReportImageRepository;
import com.ssafy.e102.domain.report.repository.HazardReportRepository;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.exception.UserErrorCode;
import com.ssafy.e102.domain.user.exception.UserException;
import com.ssafy.e102.domain.user.repository.UserRepository;
import com.ssafy.e102.global.geo.GeoPointConverter;

@Service
@Transactional(readOnly = true)
public class HazardReportService {

	private static final short REPRESENTATIVE_IMAGE_ORDER = 0;
	private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "reportId");

	private final HazardReportRepository hazardReportRepository;
	private final HazardReportImageRepository hazardReportImageRepository;
	private final UserRepository userRepository;
	private final GeoPointConverter geoPointConverter;

	public HazardReportService(
		HazardReportRepository hazardReportRepository,
		HazardReportImageRepository hazardReportImageRepository,
		UserRepository userRepository,
		GeoPointConverter geoPointConverter) {
		this.hazardReportRepository = hazardReportRepository;
		this.hazardReportImageRepository = hazardReportImageRepository;
		this.userRepository = userRepository;
		this.geoPointConverter = geoPointConverter;
	}

	@Transactional
	public HazardReportIdResponse createHazardReport(UUID userId, CreateHazardReportRequest request) {
		User user = getUser(userId);
		HazardReport hazardReport = HazardReport.create(
			user,
			request.reportType(),
			request.description(),
			geoPointConverter.toPoint(request.reportPoint()),
			request.imageUrls());
		HazardReport savedHazardReport = hazardReportRepository.save(hazardReport);
		return new HazardReportIdResponse(savedHazardReport.getReportId());
	}

	public HazardReportListResponse getMyHazardReports(UUID userId, Long cursor, int size) {
		PageRequest pageRequest = PageRequest.of(0, size, NEWEST_FIRST);
		Slice<HazardReport> hazardReports = cursor == null
			? hazardReportRepository.findAllByUser_UserId(userId, pageRequest)
			: hazardReportRepository.findAllByUser_UserIdAndReportIdLessThan(userId, cursor, pageRequest);
		return HazardReportListResponse.of(
			hazardReports.getContent(),
			size,
			hazardReports.hasNext(),
			getRepresentativeImageUrls(hazardReports.getContent()),
			geoPointConverter);
	}

	public HazardReportDetailResponse getMyHazardReportDetail(UUID userId, Long reportId) {
		HazardReport hazardReport = getHazardReport(reportId);
		validateOwner(hazardReport, userId);
		return HazardReportDetailResponse.of(hazardReport, geoPointConverter);
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

	private User getUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
	}

	private HazardReport getHazardReport(Long reportId) {
		return hazardReportRepository.findWithImagesByReportId(reportId)
			.orElseThrow(() -> new HazardReportException(HazardReportErrorCode.HAZARD_REPORT_NOT_FOUND));
	}

	private void validateOwner(HazardReport hazardReport, UUID userId) {
		if (!hazardReport.isOwner(userId)) {
			throw new HazardReportException(HazardReportErrorCode.HAZARD_REPORT_FORBIDDEN);
		}
	}
}
