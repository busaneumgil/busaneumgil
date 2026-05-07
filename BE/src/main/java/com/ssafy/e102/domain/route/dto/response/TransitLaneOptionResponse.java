package com.ssafy.e102.domain.route.dto.response;

public record TransitLaneOptionResponse(
	String routeNo,
	Integer remainingMinute,
	Integer durationSecond,
	Integer estimatedTimeMinute,
	Boolean isLowFloor) {
}
