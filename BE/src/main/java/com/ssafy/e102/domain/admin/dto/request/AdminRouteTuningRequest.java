package com.ssafy.e102.domain.admin.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AdminRouteTuningRequest(
	@NotNull @DecimalMin("0.0") @DecimalMax("30.0")
	Double slopeLowPercent,
	@NotNull @DecimalMin("0.0") @DecimalMax("30.0")
	Double slopeMiddlePercent,
	@NotNull @DecimalMin("0.0") @DecimalMax("30.0")
	Double slopeHighPercent,
	@NotNull @DecimalMin("0.0") @DecimalMax("2.0")
	Double slopeLowPenalty,
	@NotNull @DecimalMin("0.0") @DecimalMax("2.0")
	Double slopeMiddlePenalty,
	@NotNull @DecimalMin("0.0") @DecimalMax("2.0")
	Double slopeHighPenalty,
	@NotNull @DecimalMin("0.0") @DecimalMax("2.0")
	Double narrowWidthPenalty,
	@NotNull @DecimalMin("0.0") @DecimalMax("2.0")
	Double unpavedSurfacePenalty,
	@NotNull @DecimalMin("0.0") @DecimalMax("2.0")
	Double stairsPenalty,
	@NotNull @DecimalMin("0.0") @DecimalMax("2.0")
	Double signalCrosswalkBonus,
	@NotNull @DecimalMin("0.0") @DecimalMax("200.0")
	Double distanceInfluence) {
}
