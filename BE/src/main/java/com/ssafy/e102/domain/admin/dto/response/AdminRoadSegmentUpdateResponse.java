package com.ssafy.e102.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 segment 속성 수정 결과")
public record AdminRoadSegmentUpdateResponse(
	@Schema(description = "DB에 저장된 최신 segment 속성")
	AdminRoadSegmentPropertiesResponse segment,
	@Schema(description = "즉시 경로 반영 처리 상태")
	AdminRoutingApplyStatus routingApplyStatus,
	@Schema(description = "즉시 경로 반영 처리 메시지")
	String routingApplyMessage) {
}
