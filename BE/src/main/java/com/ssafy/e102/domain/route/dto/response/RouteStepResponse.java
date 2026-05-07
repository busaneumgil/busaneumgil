package com.ssafy.e102.domain.route.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.WidthState;

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
