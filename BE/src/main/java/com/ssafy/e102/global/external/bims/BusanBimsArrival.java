package com.ssafy.e102.global.external.bims;

public record BusanBimsArrival(
	String stopId,
	String lineId,
	String routeNo,
	Integer remainingMinute,
	Boolean isLowFloor) {
}
