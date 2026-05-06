package com.ssafy.e102.domain.route.dto.request;

import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

import jakarta.validation.Valid;

public record UpdateFavoriteRouteRequest(
	String startLabel,
	String endLabel,
	@Valid
	GeoPointRequest startPoint,
	@Valid
	GeoPointRequest endPoint,
	RouteOption routeOption) {

	public boolean hasAnyField() {
		return startLabel != null
			|| endLabel != null
			|| startPoint != null
			|| endPoint != null
			|| routeOption != null;
	}
}
