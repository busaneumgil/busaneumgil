package com.ssafy.e102.domain.route.dto.response;

import java.math.BigDecimal;

public record RouteStepAlertResponse(
	RouteStepAlertType type,
	BigDecimal distanceMeter) {
}
