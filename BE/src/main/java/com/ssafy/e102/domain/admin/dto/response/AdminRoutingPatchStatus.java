package com.ssafy.e102.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "실시간 GraphHopper 경로 반영 상태")
public enum AdminRoutingPatchStatus {
	SKIPPED,
	APPLIED,
	FAILED
}
