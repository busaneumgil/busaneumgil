package com.ssafy.e102.global.external.graphhopper;

import java.util.List;

public record GraphHopperRouteResponse(
	List<GraphHopperPathResponse> paths) {
}
