package com.ssafy.e102.domain.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;

public record HazardReportRerouteResponse(
	boolean rerouted,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	RouteSummaryResponse route) {
}
