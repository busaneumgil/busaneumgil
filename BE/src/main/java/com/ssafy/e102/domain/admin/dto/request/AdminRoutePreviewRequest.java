package com.ssafy.e102.domain.admin.dto.request;

import com.ssafy.e102.global.geo.dto.GeoPointRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminRoutePreviewRequest(
	@NotNull @Valid
	GeoPointRequest startPoint,
	@NotNull @Valid
	GeoPointRequest endPoint,
	@NotBlank
	String profile,
	@NotNull @Valid
	AdminRouteTuningRequest tuning) {
}
