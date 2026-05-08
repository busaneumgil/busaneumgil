package com.ssafy.e102.domain.route.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.TransportMode;

/**
 * route 안에서 같은 이동 수단으로 이어지는 구간 응답 DTO다.
 *
 * <p>도보 검색에서는 GraphHopper path 전체가 WALK leg가 되고, 다음 단계에서 steps로 세분화된다.
 */
public record RouteLegResponse(
	int sequence,
	TransportMode type,
	RouteLegRole role,
	String instruction,
	BigDecimal distanceMeter,
	int durationSecond,
	int estimatedTimeMinute,
	String geometry,
	List<RouteStepResponse> steps) {
}
