package com.ssafy.e102.global.external.graphhopper;

import java.math.BigDecimal;
import java.util.List;

public record GraphHopperPathResponse(
	BigDecimal distance,
	long time,
	GraphHopperPointsResponse points) {

	List<GraphHopperCoordinate> coordinates() {
		if (points == null || points.coordinates() == null) {
			return List.of();
		}
		return points.coordinates()
			.stream()
			.map(GraphHopperCoordinate::from)
			.toList();
	}
}
