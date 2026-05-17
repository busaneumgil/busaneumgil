package com.ssafy.e102.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 segment 속성 수정 결과")
public record AdminRoadSegmentUpdateResponse(
	@Schema(description = "DB에 저장된 최신 segment 속성")
	AdminRoadSegmentPropertiesResponse segment,
	@Schema(description = "실시간 GraphHopper 경로 반영 상태")
	AdminRoutingPatchStatus routingPatchStatus,
	@Schema(description = "실시간 GraphHopper 경로 반영 메시지")
	String routingPatchMessage) {
}
