package com.ssafy.e102.domain.route.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RouteRatingRequest(
	@NotBlank
	String routeId,

	@NotNull @Min(1) @Max(5)
	Integer score) {
}
