package com.ssafy.e102.domain.route.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.TransportMode;

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
