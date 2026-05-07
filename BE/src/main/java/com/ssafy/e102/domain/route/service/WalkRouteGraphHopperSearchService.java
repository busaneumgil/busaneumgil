package com.ssafy.e102.domain.route.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.WalkRouteProfile;
import com.ssafy.e102.domain.user.type.MobilitySubtype;
import com.ssafy.e102.domain.user.type.PrimaryUserType;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteClient;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteRequest;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

@Service
public class WalkRouteGraphHopperSearchService {

	private final WalkRouteProfileService profileService;
	private final GraphHopperRouteClient graphHopperRouteClient;

	public WalkRouteGraphHopperSearchService(
		WalkRouteProfileService profileService,
		GraphHopperRouteClient graphHopperRouteClient) {
		this.profileService = profileService;
		this.graphHopperRouteClient = graphHopperRouteClient;
	}

	public List<WalkRouteCandidate> searchCandidates(
		GeoPointRequest startPoint,
		GeoPointRequest endPoint,
		PrimaryUserType primaryUserType,
		MobilitySubtype mobilitySubtype) {
		List<WalkRouteCandidate> candidates = new ArrayList<>();
		for (RouteOption routeOption : List.of(RouteOption.SAFE, RouteOption.SHORTEST)) {
			findCandidate(startPoint, endPoint, primaryUserType, mobilitySubtype, routeOption, candidates);
		}
		if (candidates.isEmpty()) {
			throw new RouteException(RouteErrorCode.ROUTE_NOT_FOUND);
		}
		return List.copyOf(candidates);
	}

	private void findCandidate(
		GeoPointRequest startPoint,
		GeoPointRequest endPoint,
		PrimaryUserType primaryUserType,
		MobilitySubtype mobilitySubtype,
		RouteOption routeOption,
		List<WalkRouteCandidate> candidates) {
		WalkRouteProfile profile = profileService.resolve(primaryUserType, mobilitySubtype, routeOption);
		try {
			GraphHopperRoutePath path = graphHopperRouteClient.route(
				new GraphHopperRouteRequest(startPoint, endPoint, profile));
			candidates.add(new WalkRouteCandidate(routeOption, profile, path));
		} catch (RouteException exception) {
			if (exception.getErrorCode() == RouteErrorCode.ROUTE_NOT_FOUND) {
				return;
			}
			throw exception;
		}
	}
}
