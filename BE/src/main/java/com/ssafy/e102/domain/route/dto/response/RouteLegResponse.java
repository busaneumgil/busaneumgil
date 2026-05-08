package com.ssafy.e102.domain.route.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.TransportMode;

/**
 * route 안에서 같은 이동 수단으로 이어지는 구간 응답 DTO다.
 *
 * <p>도보 검색에서는 GraphHopper path 전체가 WALK leg가 되고, guidanceEvents가 안내 지점을 표현한다.
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
	List<RouteGuidanceEventResponse> guidanceEvents,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	String routeNo,
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	List<TransitLaneOptionResponse> laneOptions,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	RouteStopResponse boardingStop,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	RouteStopResponse alightingStop,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	Boolean isLowFloor,
	@JsonIgnore
	List<RouteBadge> badges) {

	public RouteLegResponse(
		int sequence,
		TransportMode type,
		RouteLegRole role,
		String instruction,
		BigDecimal distanceMeter,
		int durationSecond,
		int estimatedTimeMinute,
		String geometry,
		List<RouteGuidanceEventResponse> guidanceEvents) {
		this(
			sequence,
			type,
			role,
			instruction,
			distanceMeter,
			durationSecond,
			estimatedTimeMinute,
			geometry,
			guidanceEvents,
			null,
			List.of(),
			null,
			null,
			null,
			List.of());
	}
}
