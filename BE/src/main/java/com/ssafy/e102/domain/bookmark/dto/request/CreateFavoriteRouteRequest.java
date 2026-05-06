package com.ssafy.e102.domain.bookmark.dto.request;

import com.ssafy.e102.domain.bookmark.type.RouteOption;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFavoriteRouteRequest(
	@NotBlank(message = "출발지명은 필수입니다.")
	String startLabel,

	@NotBlank(message = "도착지명은 필수입니다.")
	String endLabel,

	@Valid @NotNull(message = "출발지 좌표는 필수입니다.")
	GeoPointRequest startPoint,

	@Valid @NotNull(message = "도착지 좌표는 필수입니다.")
	GeoPointRequest endPoint,

	@NotNull(message = "경로 옵션은 필수입니다.")
	RouteOption routeOption) {
}
