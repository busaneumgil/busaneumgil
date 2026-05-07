package com.ssafy.e102.global.external.graphhopper;

import com.ssafy.e102.domain.route.type.WalkRouteProfile;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

public record GraphHopperRouteRequest(
	GeoPointRequest startPoint,
	GeoPointRequest endPoint,
	WalkRouteProfile profile) {
}
