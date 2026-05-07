package com.ssafy.e102.domain.route.dto.response;

import java.util.List;

public record WalkRouteSearchResponse(
	String searchId,
	List<RouteSummaryResponse> routes) {
}
