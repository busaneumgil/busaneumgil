package com.ssafy.e102.domain.report.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.ssafy.e102.domain.report.dto.response.AdminHazardReportDetailResponse;
import com.ssafy.e102.domain.report.dto.response.AdminHazardReportListResponse;
import com.ssafy.e102.domain.report.dto.response.AdminHazardReportStatusResponse;
import com.ssafy.e102.domain.report.dto.response.AdminHazardReportSummaryResponse;
import com.ssafy.e102.domain.report.service.AdminHazardReportService;
import com.ssafy.e102.domain.report.type.ReportStatus;
import com.ssafy.e102.domain.report.type.ReportType;
import com.ssafy.e102.global.geo.dto.GeoPointResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

class AdminHazardReportControllerTest {

	@Mock
	private AdminHazardReportService adminHazardReportService;

	private MockMvc mockMvc;
	private UUID adminUserId;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		adminUserId = UUID.randomUUID();
		mockMvc = MockMvcBuilders.standaloneSetup(new AdminHazardReportController(adminHazardReportService))
			.setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
				@Override
				public boolean supportsParameter(MethodParameter parameter) {
					return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
						&& parameter.getParameterType().equals(AuthPrincipal.class);
				}

				@Override
				public Object resolveArgument(
					MethodParameter parameter,
					ModelAndViewContainer mavContainer,
					NativeWebRequest webRequest,
					WebDataBinderFactory binderFactory) {
					return new AuthPrincipal(adminUserId);
				}
			})
			.build();
	}

	@Test
	@DisplayName("관리자 제보 목록은 status, cursor, size를 전달한다")
	void getHazardReports() throws Exception {
		UUID reporterUserId = UUID.randomUUID();
		when(adminHazardReportService.getHazardReports(ReportStatus.PENDING, 10L, 2))
			.thenReturn(new AdminHazardReportListResponse(
				List.of(new AdminHazardReportSummaryResponse(
					3L,
					reporterUserId,
					ReportType.SIDEWALK_MISSING,
					new GeoPointResponse(35.1686, 129.0576),
					ReportStatus.PENDING,
					LocalDateTime.of(2026, 5, 7, 22, 0),
					"https://example.com/reports/3/image-1.jpg")),
				2,
				3L,
				true));

		mockMvc.perform(get("/admin/hazard-reports")
			.param("status", "PENDING")
			.param("cursor", "10")
			.param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].reportId").value(3))
			.andExpect(jsonPath("$.data.content[0].reporterUserId").value(reporterUserId.toString()))
			.andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
			.andExpect(jsonPath("$.data.nextCursor").value(3))
			.andExpect(jsonPath("$.data.hasNext").value(true));

		verify(adminHazardReportService).getHazardReports(ReportStatus.PENDING, 10L, 2);
	}

	@Test
	@DisplayName("관리자 제보 상세는 처리 상태와 전체 이미지를 반환한다")
	void getHazardReportDetail() throws Exception {
		UUID reporterUserId = UUID.randomUUID();
		when(adminHazardReportService.getHazardReportDetail(1L))
			.thenReturn(new AdminHazardReportDetailResponse(
				1L,
				reporterUserId,
				ReportType.RAMP,
				"경사로가 파손되었습니다.",
				new GeoPointResponse(35.1686, 129.0576),
				ReportStatus.PENDING,
				LocalDateTime.of(2026, 5, 7, 22, 0),
				List.of("https://example.com/reports/1/image-1.jpg")));

		mockMvc.perform(get("/admin/hazard-reports/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.reportId").value(1))
			.andExpect(jsonPath("$.data.reporterUserId").value(reporterUserId.toString()))
			.andExpect(jsonPath("$.data.status").value("PENDING"))
			.andExpect(jsonPath("$.data.imageUrls[0]").value("https://example.com/reports/1/image-1.jpg"));

		verify(adminHazardReportService).getHazardReportDetail(1L);
	}

	@Test
	@DisplayName("관리자 제보 승인은 변경된 상태를 반환한다")
	void approveHazardReport() throws Exception {
		when(adminHazardReportService.approveHazardReport(adminUserId, 1L))
			.thenReturn(new AdminHazardReportStatusResponse(1L, ReportStatus.APPROVED));

		mockMvc.perform(patch("/admin/hazard-reports/1/approve"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.reportId").value(1))
			.andExpect(jsonPath("$.data.status").value("APPROVED"));

		verify(adminHazardReportService).approveHazardReport(adminUserId, 1L);
	}

	@Test
	@DisplayName("관리자 제보 반려는 변경된 상태를 반환한다")
	void rejectHazardReport() throws Exception {
		when(adminHazardReportService.rejectHazardReport(adminUserId, 1L))
			.thenReturn(new AdminHazardReportStatusResponse(1L, ReportStatus.REJECTED));

		mockMvc.perform(patch("/admin/hazard-reports/1/reject"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.reportId").value(1))
			.andExpect(jsonPath("$.data.status").value("REJECTED"));

		verify(adminHazardReportService).rejectHazardReport(adminUserId, 1L);
	}
}
