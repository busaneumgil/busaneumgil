package com.ssafy.e102.domain.route.dto.request;

import com.ssafy.e102.global.geo.dto.GeoPointRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record WalkRouteSearchRequest(
	@Valid @NotNull(message = "출발지 좌표는 필수입니다.")
	GeoPointRequest startPoint,

	@Valid @NotNull(message = "도착지 좌표는 필수입니다.")
	GeoPointRequest endPoint) {
}
