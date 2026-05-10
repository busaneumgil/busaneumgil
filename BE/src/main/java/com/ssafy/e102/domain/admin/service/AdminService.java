package com.ssafy.e102.domain.admin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.admin.dto.response.AdminMeResponse;

@Service
@Transactional(readOnly = true)
public class AdminService {

	private static final List<String> ADMIN_PERMISSIONS = List.of(
		"HAZARD_REPORT_READ",
		"HAZARD_REPORT_REVIEW");

	public AdminMeResponse getMe(UUID userId) {
		return AdminMeResponse.of(userId, ADMIN_PERMISSIONS);
	}
}
