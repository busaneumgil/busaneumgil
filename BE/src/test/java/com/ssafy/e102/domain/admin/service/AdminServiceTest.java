package com.ssafy.e102.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ssafy.e102.domain.admin.dto.response.AdminMeResponse;

class AdminServiceTest {

	private final AdminService adminService = new AdminService();

	@Test
	@DisplayName("관리자 principal은 관리자 permission을 조회할 수 있다")
	void getAdminMe() {
		UUID userId = UUID.randomUUID();

		AdminMeResponse response = adminService.getMe(userId);

		assertThat(response.userId()).isEqualTo(userId);
		assertThat(response.role()).isEqualTo("ADMIN");
		assertThat(response.permissions()).containsExactly("HAZARD_REPORT_READ", "HAZARD_REPORT_REVIEW");
	}
}
