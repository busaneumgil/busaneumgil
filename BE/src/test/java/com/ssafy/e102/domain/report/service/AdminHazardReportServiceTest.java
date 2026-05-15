package com.ssafy.e102.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.e102.domain.report.dto.response.AdminHazardReportDetailResponse;
import com.ssafy.e102.domain.report.dto.response.AdminHazardReportListResponse;
import com.ssafy.e102.domain.report.dto.response.AdminHazardReportStatusResponse;
import com.ssafy.e102.domain.report.entity.HazardReport;
import com.ssafy.e102.domain.report.exception.HazardReportErrorCode;
import com.ssafy.e102.domain.report.exception.HazardReportException;
import com.ssafy.e102.domain.report.repository.HazardReportImageRepository;
import com.ssafy.e102.domain.report.repository.HazardReportRepository;
import com.ssafy.e102.domain.report.type.ReportStatus;
import com.ssafy.e102.domain.report.type.ReportType;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.type.PrimaryUserType;
import com.ssafy.e102.domain.user.type.SocialProvider;
import com.ssafy.e102.global.geo.GeoPointConverter;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

class AdminHazardReportServiceTest {

	@Mock
	private HazardReportRepository hazardReportRepository;

	@Mock
	private HazardReportImageRepository hazardReportImageRepository;

	@Mock
	private HazardReportImageUploadService hazardReportImageUploadService;

	private AdminHazardReportService adminHazardReportService;
	private GeoPointConverter geoPointConverter;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		geoPointConverter = new GeoPointConverter();
		adminHazardReportService = new AdminHazardReportService(
			hazardReportRepository,
			hazardReportImageRepository,
			geoPointConverter,
			hazardReportImageUploadService);
	}

	@Test
	@DisplayName("관리자 제보 목록은 status와 cursor 기준으로 조회한다")
	void getHazardReports() {
		HazardReport hazardReport = hazardReport(user(UUID.randomUUID()), 3L,
			List.of("hazard-reports/user-3/20260514/image-1.jpg"));
		PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "reportId"));
		when(hazardReportRepository.findAllByStatusAndReportIdLessThanForAdmin(
			ReportStatus.PENDING,
			10L,
			pageable))
			.thenReturn(new SliceImpl<>(List.of(hazardReport), pageable, true));
		when(hazardReportImageRepository.findAllByHazardReport_ReportIdInAndDisplayOrder(List.of(3L), (short)0))
			.thenReturn(List.of(hazardReport.getImages().get(0)));
		when(hazardReportImageUploadService.createReadUrl("hazard-reports/user-3/20260514/image-1.jpg"))
			.thenReturn("https://storage.example.com/read?key=image-1");

		AdminHazardReportListResponse response = adminHazardReportService.getHazardReports(
			ReportStatus.PENDING,
			10L,
			2);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).reportId()).isEqualTo(3L);
		assertThat(response.content().get(0).address()).isEqualTo("부산 부산진구 시민공원로 73");
		assertThat(response.content().get(0).description()).isEqualTo("보행 가능한 인도가 없습니다.");
		assertThat(response.content().get(0).status()).isEqualTo(ReportStatus.PENDING);
		assertThat(response.content().get(0).representativeImageUrl())
			.isEqualTo("https://storage.example.com/read?key=image-1");
		assertThat(response.nextCursor()).isEqualTo(3L);
		assertThat(response.hasNext()).isTrue();
	}

	@Test
	@DisplayName("관리자 제보 상세는 처리 상태와 조회용 presigned 이미지 URL을 반환한다")
	void getHazardReportDetail() {
		UUID reporterUserId = UUID.randomUUID();
		HazardReport hazardReport = hazardReport(user(reporterUserId), 1L, List.of(
			"hazard-reports/user-1/20260514/image-1.jpg",
			"hazard-reports/user-1/20260514/image-2.jpg"));
		when(hazardReportRepository.findWithImagesAndUserByReportId(1L)).thenReturn(Optional.of(hazardReport));
		when(hazardReportImageUploadService.createReadUrl("hazard-reports/user-1/20260514/image-1.jpg"))
			.thenReturn("https://storage.example.com/read?key=image-1");
		when(hazardReportImageUploadService.createReadUrl("hazard-reports/user-1/20260514/image-2.jpg"))
			.thenReturn("https://storage.example.com/read?key=image-2");

		AdminHazardReportDetailResponse response = adminHazardReportService.getHazardReportDetail(1L);

		assertThat(response.reportId()).isEqualTo(1L);
		assertThat(response.reporterUserId()).isEqualTo(reporterUserId);
		assertThat(response.address()).isEqualTo("부산 부산진구 시민공원로 73");
		assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
		assertThat(response.imageUrls()).containsExactly(
			"https://storage.example.com/read?key=image-1",
			"https://storage.example.com/read?key=image-2");
	}

	@Test
	@DisplayName("관리자 제보 승인은 PENDING 제보를 APPROVED로 변경한다")
	void approveHazardReport() {
		when(hazardReportRepository.updateStatusIfCurrentStatus(
			1L,
			ReportStatus.PENDING,
			ReportStatus.APPROVED))
			.thenReturn(1);

		AdminHazardReportStatusResponse response = adminHazardReportService.approveHazardReport(1L);

		assertThat(response.reportId()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(ReportStatus.APPROVED);
	}

	@Test
	@DisplayName("관리자 제보 반려는 PENDING 제보를 REJECTED로 변경한다")
	void rejectHazardReport() {
		when(hazardReportRepository.updateStatusIfCurrentStatus(
			1L,
			ReportStatus.PENDING,
			ReportStatus.REJECTED))
			.thenReturn(1);

		AdminHazardReportStatusResponse response = adminHazardReportService.rejectHazardReport(1L);

		assertThat(response.reportId()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(ReportStatus.REJECTED);
	}

	@Test
	@DisplayName("이미 처리된 제보는 다시 승인할 수 없다")
	void rejectAlreadyProcessedReport() {
		when(hazardReportRepository.updateStatusIfCurrentStatus(
			1L,
			ReportStatus.PENDING,
			ReportStatus.APPROVED))
			.thenReturn(0);
		when(hazardReportRepository.existsById(1L)).thenReturn(true);

		assertThatThrownBy(() -> adminHazardReportService.approveHazardReport(1L))
			.isInstanceOf(HazardReportException.class)
			.extracting("errorCode")
			.isEqualTo(HazardReportErrorCode.HAZARD_REPORT_ALREADY_PROCESSED);
	}

	@Test
	@DisplayName("존재하지 않는 제보는 승인할 수 없다")
	void rejectUnknownReportStatusUpdate() {
		when(hazardReportRepository.updateStatusIfCurrentStatus(
			1L,
			ReportStatus.PENDING,
			ReportStatus.APPROVED))
			.thenReturn(0);
		when(hazardReportRepository.existsById(1L)).thenReturn(false);

		assertThatThrownBy(() -> adminHazardReportService.approveHazardReport(1L))
			.isInstanceOf(HazardReportException.class)
			.extracting("errorCode")
			.isEqualTo(HazardReportErrorCode.HAZARD_REPORT_NOT_FOUND);
	}

	private HazardReport hazardReport(User user, Long reportId, List<String> imageObjectKeys) {
		HazardReport hazardReport = HazardReport.create(
			user,
			ReportType.SIDEWALK_MISSING,
			"보행 가능한 인도가 없습니다.",
			"부산 부산진구 시민공원로 73",
			geoPointConverter.toPoint(new GeoPointRequest(35.1686, 129.0576)),
			imageObjectKeys);
		ReflectionTestUtils.setField(hazardReport, "reportId", reportId);
		ReflectionTestUtils.setField(hazardReport, "createdAt", LocalDateTime.of(2026, 5, 7, 22, 0));
		return hazardReport;
	}

	private User user(UUID userId) {
		User user = User.create(SocialProvider.KAKAO, "kakao-user-id", PrimaryUserType.LOW_VISION, null);
		ReflectionTestUtils.setField(user, "userId", userId);
		return user;
	}
}
