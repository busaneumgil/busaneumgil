package com.ssafy.e102.domain.route.dto.response;

import java.math.BigDecimal;

/**
 * leg 시작점과 route 시작점 기준 누적 거리/시간으로 표현하는 안내 이벤트 응답 DTO다.
 */
public record RouteGuidanceEventResponse(
	int sequence,
	RouteGuidanceEventType type,
	BigDecimal distanceFromLegStartMeter,
	int durationFromLegStartSecond,
	BigDecimal distanceFromRouteStartMeter,
	int durationFromRouteStartSecond,
	String geometry) {

	public RouteGuidanceEventResponse(
		int sequence,
		RouteGuidanceEventType type,
		BigDecimal distanceFromLegStartMeter,
		int durationFromLegStartSecond,
		String geometry) {
		this(
			sequence,
			type,
			distanceFromLegStartMeter,
			durationFromLegStartSecond,
			distanceFromLegStartMeter,
			durationFromLegStartSecond,
			geometry);
	}
}
