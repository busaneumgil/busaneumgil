package com.ssafy.e102.global.external.graphhopper;

import java.math.BigDecimal;
import java.util.List;

public record GraphHopperRoutePath(
	BigDecimal distanceMeter,
	long timeMs,
	List<GraphHopperCoordinate> coordinates) {
}
