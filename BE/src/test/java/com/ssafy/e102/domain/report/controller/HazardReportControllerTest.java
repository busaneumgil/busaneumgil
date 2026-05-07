package com.ssafy.e102.domain.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ssafy.e102.domain.report.dto.request.CreateHazardReportRequest;
import com.ssafy.e102.domain.report.dto.response.HazardReportDetailResponse;
import com.ssafy.e102.domain.report.dto.response.HazardReportIdResponse;
import com.ssafy.e102.domain.report.dto.response.HazardReportListResponse;
import com.ssafy.e102.domain.report.dto.response.HazardReportSummaryResponse;
import com.ssafy.e102.domain.report.service.HazardReportService;
import com.ssafy.e102.domain.report.type.ReportType;
import com.ssafy.e102.global.geo.dto.GeoPointResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

class HazardReportControllerTest {

	@Mock
	private HazardReportService hazardReportService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(new HazardReportController(hazardReportService))
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.build();
	}

	@Test
	@DisplayName("도로 상태 제보 등록은 현재 사용자와 요청 본문으로 생성 응답을 반환한다")
	void createHazardReport() throws Exception {
		UUID userId = UUID.randomUUID();
		UsernamePasswordAuthenticationToken authentication = authentication(userId);
		when(hazardReportService.createHazardReport(eq(userId), any(CreateHazardReportRequest.class)))
			.thenReturn(new HazardReportIdResponse(1L));

		mockMvc.perform(post("/hazard-reports")
			.principal(authentication)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"reportType\":\"SIDEWALK_MISSING\",\"description\":\"보행 가능한 인도가 없습니다.\","
				+ "\"reportPoint\":{\"lat\":35.1686,\"lng\":129.0576},"
				+ "\"imageUrls\":[\"https://example.com/reports/1/image-1.jpg\"]}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("S2010"))
			.andExpect(jsonPath("$.data.reportId").value(1));

		verify(hazardReportService).createHazardReport(eq(userId), any(CreateHazardReportRequest.class));
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("내 제보 목록은 현재 사용자의 cursor 목록을 반환한다")
	void getMyHazardReports() throws Exception {
		UUID userId = UUID.randomUUID();
		UsernamePasswordAuthenticationToken authentication = authentication(userId);
		when(hazardReportService.getMyHazardReports(userId, null, 10))
			.thenReturn(new HazardReportListResponse(
				List.of(new HazardReportSummaryResponse(
					1L,
					ReportType.SIDEWALK_MISSING,
					new GeoPointResponse(35.1686, 129.0576),
					LocalDateTime.of(2026, 4, 28, 17, 0),
					"https://example.com/reports/1/image-1.jpg")),
				10,
				null,
				false));

		mockMvc.perform(get("/hazard-reports/me")
			.principal(authentication))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("S2000"))
			.andExpect(jsonPath("$.data.content[0].reportId").value(1))
			.andExpect(jsonPath("$.data.content[0].reportType").value("SIDEWALK_MISSING"))
			.andExpect(jsonPath("$.data.content[0].reportPoint.lat").value(35.1686))
			.andExpect(jsonPath("$.data.content[0].representativeImageUrl")
				.value("https://example.com/reports/1/image-1.jpg"))
			.andExpect(jsonPath("$.data.nextCursor").doesNotExist())
			.andExpect(jsonPath("$.data.hasNext").value(false));

		verify(hazardReportService).getMyHazardReports(userId, null, 10);
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("내 제보 목록은 cursor와 size를 전달한다")
	void getMyHazardReportsWithCursor() throws Exception {
		UUID userId = UUID.randomUUID();
		UsernamePasswordAuthenticationToken authentication = authentication(userId);
		when(hazardReportService.getMyHazardReports(userId, 10L, 2))
			.thenReturn(new HazardReportListResponse(
				List.of(new HazardReportSummaryResponse(
					3L,
					ReportType.RAMP,
					new GeoPointResponse(35.1686, 129.0576),
					LocalDateTime.of(2026, 4, 28, 17, 0),
					null)),
				2,
				3L,
				true));

		mockMvc.perform(get("/hazard-reports/me")
			.param("cursor", "10")
			.param("size", "2")
			.principal(authentication))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].reportId").value(3))
			.andExpect(jsonPath("$.data.nextCursor").value(3))
			.andExpect(jsonPath("$.data.hasNext").value(true));

		verify(hazardReportService).getMyHazardReports(userId, 10L, 2);
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("내 제보 상세는 전체 이미지 URL을 반환한다")
	void getMyHazardReportDetail() throws Exception {
		UUID userId = UUID.randomUUID();
		UsernamePasswordAuthenticationToken authentication = authentication(userId);
		when(hazardReportService.getMyHazardReportDetail(userId, 1L))
			.thenReturn(new HazardReportDetailResponse(
				1L,
				ReportType.SIDEWALK_MISSING,
				"보행 가능한 인도가 없습니다.",
				new GeoPointResponse(35.1686, 129.0576),
				LocalDateTime.of(2026, 4, 28, 17, 0),
				List.of(
					"https://example.com/reports/1/image-1.jpg",
					"https://example.com/reports/1/image-2.jpg")));

		mockMvc.perform(get("/hazard-reports/me/1")
			.principal(authentication))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("S2000"))
			.andExpect(jsonPath("$.data.reportId").value(1))
			.andExpect(jsonPath("$.data.imageUrls[0]").value("https://example.com/reports/1/image-1.jpg"))
			.andExpect(jsonPath("$.data.imageUrls[1]").value("https://example.com/reports/1/image-2.jpg"));

		verify(hazardReportService).getMyHazardReportDetail(userId, 1L);
		SecurityContextHolder.clearContext();
	}

	private UsernamePasswordAuthenticationToken authentication(UUID userId) {
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			new AuthPrincipal(userId, "access-token"), null);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		return authentication;
	}
}
