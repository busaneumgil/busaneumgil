package com.ssafy.e102.domain.route.service;

import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.WalkRouteProfile;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;

public record WalkRouteCandidate(
	RouteOption routeOption,
	WalkRouteProfile profile,
	GraphHopperRoutePath path) {
}
