package com.ssafy.e102.domain.route.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;

public record RouteSummaryResponse(
	String routeId,
	TransportMode transportMode,
	RouteOption routeOption,
	String title,
	BigDecimal distanceMeter,
	int durationSecond,
	int estimatedTimeMinute,
	List<RouteBadge> badges,
	String geometry,
	List<RouteLegResponse> legs) {
}
