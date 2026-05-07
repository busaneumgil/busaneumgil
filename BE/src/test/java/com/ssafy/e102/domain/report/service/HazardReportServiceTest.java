package com.ssafy.e102.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.ssafy.e102.domain.report.dto.request.CreateHazardReportRequest;
import com.ssafy.e102.domain.report.dto.response.HazardReportDetailResponse;
import com.ssafy.e102.domain.report.dto.response.HazardReportIdResponse;
import com.ssafy.e102.domain.report.dto.response.HazardReportListResponse;
import com.ssafy.e102.domain.report.entity.HazardReport;
import com.ssafy.e102.domain.report.exception.HazardReportErrorCode;
import com.ssafy.e102.domain.report.exception.HazardReportException;
import com.ssafy.e102.domain.report.repository.HazardReportImageRepository;
import com.ssafy.e102.domain.report.repository.HazardReportRepository;
import com.ssafy.e102.domain.report.type.ReportType;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.repository.UserRepository;
import com.ssafy.e102.domain.user.type.PrimaryUserType;
import com.ssafy.e102.domain.user.type.SocialProvider;
import com.ssafy.e102.global.geo.GeoPointConverter;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

class HazardReportServiceTest {

	@Mock
	private HazardReportRepository hazardReportRepository;

	@Mock
	private HazardReportImageRepository hazardReportImageRepository;

	@Mock
	private UserRepository userRepository;

	private HazardReportService hazardReportService;
	private GeoPointConverter geoPointConverter;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		geoPointConverter = new GeoPointConverter();
		hazardReportService = new HazardReportService(
			hazardReportRepository,
			hazardReportImageRepository,
			userRepository,
			geoPointConverter);
	}

	@Test
	@DisplayName("도로 상태 제보 등록은 현재 사용자와 위치를 저장한다")
	void createHazardReport() {
		UUID userId = UUID.randomUUID();
		User user = user(userId);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(hazardReportRepository.save(any(HazardReport.class))).thenAnswer(invocation -> {
			HazardReport hazardReport = invocation.getArgument(0);
			ReflectionTestUtils.setField(hazardReport, "reportId", 1L);
			return hazardReport;
		});

		HazardReportIdResponse response = hazardReportService.createHazardReport(
			userId,
			new CreateHazardReportRequest(
				ReportType.SIDEWALK_MISSING,
				"보행 가능한 인도가 없습니다.",
				new GeoPointRequest(35.1686, 129.0576),
				List.of("https://example.com/reports/1/image-1.jpg")));

		assertThat(response.reportId()).isEqualTo(1L);
		verify(hazardReportRepository).save(any(HazardReport.class));
	}

	@Test
	@DisplayName("사용자가 없으면 도로 상태 제보를 등록하지 않는다")
	void rejectCreateWithoutUser() {
		UUID userId = UUID.randomUUID();
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> hazardReportService.createHazardReport(
			userId,
			new CreateHazardReportRequest(
				ReportType.SIDEWALK_MISSING,
				null,
				new GeoPointRequest(35.1686, 129.0576),
				null)))
			.hasMessage("사용자를 찾을 수 없습니다.");

		verify(hazardReportRepository, never()).save(any(HazardReport.class));
	}

	@Test
	@DisplayName("내 제보 목록은 최신 저장순 cursor 기반으로 대표 이미지를 함께 조회한다")
	void getMyHazardReports() {
		UUID userId = UUID.randomUUID();
		HazardReport hazardReport = hazardReport(user(userId), 1L,
			List.of("https://example.com/reports/1/image-1.jpg"));
		PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "reportId"));
		when(hazardReportRepository.findAllByUser_UserId(userId, pageable))
			.thenReturn(new SliceImpl<>(List.of(hazardReport), pageable, false));
		when(hazardReportImageRepository.findAllByHazardReport_ReportIdInAndDisplayOrder(List.of(1L), (short)0))
			.thenReturn(List.of(hazardReport.getImages().get(0)));

		HazardReportListResponse response = hazardReportService.getMyHazardReports(userId, null, 10);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).reportId()).isEqualTo(1L);
		assertThat(response.content().get(0).reportType()).isEqualTo(ReportType.SIDEWALK_MISSING);
		assertThat(response.content().get(0).representativeImageUrl())
			.isEqualTo("https://example.com/reports/1/image-1.jpg");
		assertThat(response.nextCursor()).isNull();
		assertThat(response.hasNext()).isFalse();
	}

	@Test
	@DisplayName("내 제보 목록은 마지막 제보 ID 이후 cursor로 조회한다")
	void getMyHazardReportsWithCursor() {
		UUID userId = UUID.randomUUID();
		HazardReport hazardReport = hazardReport(user(userId), 3L, List.of());
		PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "reportId"));
		when(hazardReportRepository.findAllByUser_UserIdAndReportIdLessThan(userId, 10L, pageable))
			.thenReturn(new SliceImpl<>(List.of(hazardReport), pageable, true));

		HazardReportListResponse response = hazardReportService.getMyHazardReports(userId, 10L, 2);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).representativeImageUrl()).isNull();
		assertThat(response.nextCursor()).isEqualTo(3L);
		assertThat(response.hasNext()).isTrue();
	}

	@Test
	@DisplayName("내 제보 상세는 전체 이미지 URL을 반환한다")
	void getMyHazardReportDetail() {
		UUID userId = UUID.randomUUID();
		HazardReport hazardReport = hazardReport(user(userId), 1L, List.of(
			"https://example.com/reports/1/image-1.jpg",
			"https://example.com/reports/1/image-2.jpg"));
		when(hazardReportRepository.findWithImagesByReportId(1L)).thenReturn(Optional.of(hazardReport));

		HazardReportDetailResponse response = hazardReportService.getMyHazardReportDetail(userId, 1L);

		assertThat(response.reportId()).isEqualTo(1L);
		assertThat(response.imageUrls()).containsExactly(
			"https://example.com/reports/1/image-1.jpg",
			"https://example.com/reports/1/image-2.jpg");
	}

	@Test
	@DisplayName("다른 사용자의 제보 상세 조회는 거부한다")
	void rejectOtherUserHazardReportDetail() {
		HazardReport hazardReport = hazardReport(user(UUID.randomUUID()), 1L, List.of());
		when(hazardReportRepository.findWithImagesByReportId(1L)).thenReturn(Optional.of(hazardReport));

		assertThatThrownBy(() -> hazardReportService.getMyHazardReportDetail(UUID.randomUUID(), 1L))
			.isInstanceOf(HazardReportException.class)
			.extracting("errorCode")
			.isEqualTo(HazardReportErrorCode.HAZARD_REPORT_FORBIDDEN);
	}

	@Test
	@DisplayName("존재하지 않는 제보 상세 조회는 거부한다")
	void rejectMissingHazardReportDetail() {
		when(hazardReportRepository.findWithImagesByReportId(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> hazardReportService.getMyHazardReportDetail(UUID.randomUUID(), 1L))
			.isInstanceOf(HazardReportException.class)
			.extracting("errorCode")
			.isEqualTo(HazardReportErrorCode.HAZARD_REPORT_NOT_FOUND);
	}

	private HazardReport hazardReport(User user, Long reportId, List<String> imageUrls) {
		HazardReport hazardReport = HazardReport.create(
			user,
			ReportType.SIDEWALK_MISSING,
			"보행 가능한 인도가 없습니다.",
			geoPointConverter.toPoint(new GeoPointRequest(35.1686, 129.0576)),
			imageUrls);
		ReflectionTestUtils.setField(hazardReport, "reportId", reportId);
		ReflectionTestUtils.setField(hazardReport, "createdAt", LocalDateTime.of(2026, 4, 28, 17, 0));
		return hazardReport;
	}

	private User user(UUID userId) {
		User user = User.create(SocialProvider.KAKAO, "kakao-user-id", PrimaryUserType.LOW_VISION, null);
		ReflectionTestUtils.setField(user, "userId", userId);
		return user;
	}
}
