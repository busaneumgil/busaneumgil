package com.ssafy.e102.global.external.graphhopper;

import java.math.BigDecimal;
import java.util.List;

public record GraphHopperCoordinate(
	BigDecimal lng,
	BigDecimal lat) {

	static GraphHopperCoordinate from(List<BigDecimal> coordinate) {
		if (coordinate == null || coordinate.size() < 2) {
			throw new IllegalArgumentException("GraphHopper coordinate must contain lng and lat.");
		}
		return new GraphHopperCoordinate(coordinate.get(0), coordinate.get(1));
	}
}
