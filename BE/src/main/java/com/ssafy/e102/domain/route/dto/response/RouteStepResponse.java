package com.ssafy.e102.domain.route.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.WidthState;

/**
 * 안내 화면과 TTS가 소비하는 route step 응답 DTO다.
 *
 * <p>segment_features, 회전 이벤트, 인접 segment 병합 결과가 이 DTO로 모이며 원천 segment와 1:1이라고 가정하지 않는다.
 */
public record RouteStepResponse(
	int sequence,
	String instruction,
	BigDecimal distanceMeter,
	int durationSecond,
	String geometry,
	List<RouteBadge> badges,
	RouteStepAlertResponse alert,
	BigDecimal slopePercent,
	WidthState widthState) {
}
