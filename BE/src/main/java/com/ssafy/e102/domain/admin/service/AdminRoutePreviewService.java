package com.ssafy.e102.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.admin.dto.request.AdminRoutePreviewRequest;
import com.ssafy.e102.domain.admin.dto.response.AdminRoutePreviewItemResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoutePreviewResponse;
import com.ssafy.e102.domain.route.type.WalkRouteProfile;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteClient;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteRequest;
import com.ssafy.e102.global.geo.dto.GeoPointResponse;

@Service
@Transactional(readOnly = true)
public class AdminRoutePreviewService {

	private final GraphHopperRouteClient graphHopperRouteClient;

	public AdminRoutePreviewService(GraphHopperRouteClient graphHopperRouteClient) {
		this.graphHopperRouteClient = graphHopperRouteClient;
	}

	public AdminRoutePreviewResponse preview(AdminRoutePreviewRequest request) {
		WalkRouteProfile safeProfile = request.profileGroup()
			.getSafeProfile();
		WalkRouteProfile fastProfile = request.profileGroup()
			.getFastProfile();
		GraphHopperRoutePath safeRoute = graphHopperRouteClient.route(new GraphHopperRouteRequest(
			request.startPoint(),
			request.endPoint(),
			safeProfile));
		GraphHopperRoutePath fastRoute = graphHopperRouteClient.route(new GraphHopperRouteRequest(
			request.startPoint(),
			request.endPoint(),
			fastProfile));
		return new AdminRoutePreviewResponse(
			toResponse(safeProfile, safeRoute),
			toResponse(fastProfile, fastRoute));
	}

	private AdminRoutePreviewItemResponse toResponse(WalkRouteProfile profile, GraphHopperRoutePath route) {
		return new AdminRoutePreviewItemResponse(
			profile.name(),
			route.distanceMeter(),
			(int)(route.timeMs() / 1000),
			(int)Math.ceil(route.timeMs() / 60_000.0),
			route.coordinates()
				.stream()
				.map(coordinate -> new GeoPointResponse(coordinate.lat().doubleValue(), coordinate.lng().doubleValue()))
				.toList());
	}
}
