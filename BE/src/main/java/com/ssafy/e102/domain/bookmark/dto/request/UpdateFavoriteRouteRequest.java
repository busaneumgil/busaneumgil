package com.ssafy.e102.domain.bookmark.dto.request;

import com.ssafy.e102.domain.bookmark.type.RouteOption;
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
