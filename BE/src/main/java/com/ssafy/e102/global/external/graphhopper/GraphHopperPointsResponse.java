package com.ssafy.e102.global.external.graphhopper;

import java.math.BigDecimal;
import java.util.List;

public record GraphHopperPointsResponse(
	String type,
	List<List<BigDecimal>> coordinates) {
}
