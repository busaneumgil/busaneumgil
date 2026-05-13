package com.ssafy.e102.domain.route.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record LowFloorBusReservationResponse(
	String stopName,
	String arsNo,
	String routeNo,
	String vehicleNo,
	Integer remainingMinute,
	Integer remainingStopCount,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	String requestUrl) {
}
