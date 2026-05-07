package com.ssafy.e102.domain.route.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ssafy.e102.domain.route.dto.request.WalkRouteSearchRequest;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteStopResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.TransitLaneOptionResponse;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteClient;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteRequest;
import com.ssafy.e102.global.external.odsay.OdsayClient;
import com.ssafy.e102.global.external.odsay.OdsayLaneGeometry;
import com.ssafy.e102.global.external.odsay.OdsayPassStop;
import com.ssafy.e102.global.external.odsay.OdsayTransitLane;
import com.ssafy.e102.global.external.odsay.OdsayTransitLeg;
import com.ssafy.e102.global.external.odsay.OdsayTransitPath;
import com.ssafy.e102.global.external.odsay.OdsayTransitSearchResult;
import com.ssafy.e102.global.geo.GeoDistanceCalculator;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

@Service
public class TransitRouteSearchService {

	private static final double BUSAN_MIN_LAT = 34.85;
	private static final double BUSAN_MAX_LAT = 35.45;
	private static final double BUSAN_MIN_LNG = 128.70;
	private static final double BUSAN_MAX_LNG = 129.40;
	private static final double START_END_MIN_DISTANCE_METER = 20.0;

	private final WalkRouteUserProfileQueryService userProfileQueryService;
	private final WalkRouteProfileService walkRouteProfileService;
	private final WalkRoutePayloadService walkRoutePayloadService;
	private final GraphHopperRouteClient graphHopperRouteClient;
	private final OdsayClient odsayClient;
	private final RouteSearchCacheService routeSearchCacheService;

	public TransitRouteSearchService(
		WalkRouteUserProfileQueryService userProfileQueryService,
		WalkRouteProfileService walkRouteProfileService,
		WalkRoutePayloadService walkRoutePayloadService,
		GraphHopperRouteClient graphHopperRouteClient,
		OdsayClient odsayClient,
		RouteSearchCacheService routeSearchCacheService) {
		this.userProfileQueryService = userProfileQueryService;
		this.walkRouteProfileService = walkRouteProfileService;
		this.walkRoutePayloadService = walkRoutePayloadService;
		this.graphHopperRouteClient = graphHopperRouteClient;
		this.odsayClient = odsayClient;
		this.routeSearchCacheService = routeSearchCacheService;
	}

	public WalkRouteSearchResponse search(UUID userId, WalkRouteSearchRequest request) {
		WalkRouteUserProfile profile = userProfileQueryService.getProfile(userId);
		GeoPointRequest startPoint = request.startPoint();
		GeoPointRequest endPoint = request.endPoint();
		validateServiceArea(startPoint, endPoint);
		validateStartEndDistance(startPoint, endPoint);

		OdsayTransitSearchResult searchResult = odsayClient.searchPubTransPath(startPoint, endPoint);
		String searchId = "rs_transit_" + UUID.randomUUID();
		List<TransitRouteCandidate> candidates = new ArrayList<>();
		for (int index = 0; index < searchResult.paths().size(); index++) {
			OdsayTransitPath path = searchResult.paths().get(index);
			List<OdsayLaneGeometry> laneGeometries = odsayClient.loadLane(path.mapObj());
			toCandidate(searchId, index + 1, startPoint, endPoint, path, laneGeometries, profile)
				.ifPresent(candidates::add);
		}
		if (candidates.isEmpty()) {
			throw new RouteException(RouteErrorCode.ROUTE_NOT_FOUND);
		}
		WalkRouteSearchResponse response = new WalkRouteSearchResponse(searchId,
			candidates.stream().map(TransitRouteCandidate::route).toList());
		routeSearchCacheService.save(response);
		routeSearchCacheService.saveTransitMetadata(searchId,
			candidates.stream().map(TransitRouteCandidate::snapshot).toList());
		return response;
	}

	private java.util.Optional<TransitRouteCandidate> toCandidate(
		String searchId,
		int routeIndex,
		GeoPointRequest startPoint,
		GeoPointRequest endPoint,
		OdsayTransitPath path,
		List<OdsayLaneGeometry> laneGeometries,
		WalkRouteUserProfile profile) {
		String routeId = "%s_%03d".formatted(searchId, routeIndex);
		List<RouteLegResponse> legs = toLegs(startPoint, endPoint, path.legs(), laneGeometries, profile);
		if (legs.isEmpty()) {
			return java.util.Optional.empty();
		}
		RouteSummaryResponse route = new RouteSummaryResponse(
			routeId,
			TransportMode.PUBLIC_TRANSIT,
			RouteOption.RECOMMENDED,
			List.of(RouteOption.RECOMMENDED),
			title(path.legs()),
			scale(path.totalDistanceMeter()),
			path.totalTimeMinute() * 60,
			Math.max(1, path.totalTimeMinute()),
			transferCount(path),
			List.of(),
			mergeGeometry(legs),
			legs);
		return java.util.Optional.of(
			new TransitRouteCandidate(route, new TransitRouteSnapshot(routeId, path.mapObj(), snapshotLegs(path))));
	}

	private List<RouteLegResponse> toLegs(
		GeoPointRequest startPoint,
		GeoPointRequest endPoint,
		List<OdsayTransitLeg> odsayLegs,
		List<OdsayLaneGeometry> laneGeometries,
		WalkRouteUserProfile profile) {
		List<RouteLegResponse> legs = new ArrayList<>();
		GeoPointRequest cursor = startPoint;
		int[] geometryIndex = {0};
		for (OdsayTransitLeg odsayLeg : odsayLegs) {
			if (odsayLeg.type() == TransportMode.WALK) {
				GeoPointRequest nextPoint = nextTransitStart(odsayLegs, odsayLeg, endPoint);
				RouteLegResponse walkLeg = toWalkLeg(legs.size() + 1, cursor, nextPoint, odsayLeg, profile);
				if (walkLeg == null) {
					return List.of();
				}
				legs.add(walkLeg);
				cursor = nextPoint;
				continue;
			}
			RouteLegResponse transitLeg = toTransitLeg(
				legs.size() + 1,
				odsayLeg,
				nextGeometry(odsayLeg.type(), laneGeometries, geometryIndex));
			legs.add(transitLeg);
			if (odsayLeg.endLat() != null && odsayLeg.endLng() != null) {
				cursor = new GeoPointRequest(odsayLeg.endLat().doubleValue(), odsayLeg.endLng().doubleValue());
			}
		}
		return List.copyOf(legs);
	}

	private GeoPointRequest nextTransitStart(
		List<OdsayTransitLeg> odsayLegs,
		OdsayTransitLeg currentLeg,
		GeoPointRequest endPoint) {
		int currentIndex = odsayLegs.indexOf(currentLeg);
		for (int index = currentIndex + 1; index < odsayLegs.size(); index++) {
			OdsayTransitLeg next = odsayLegs.get(index);
			if (next.type() != TransportMode.WALK && next.startLat() != null && next.startLng() != null) {
				return new GeoPointRequest(next.startLat().doubleValue(), next.startLng().doubleValue());
			}
		}
		return endPoint;
	}

	private RouteLegResponse toWalkLeg(
		int sequence,
		GeoPointRequest from,
		GeoPointRequest to,
		OdsayTransitLeg odsayLeg,
		WalkRouteUserProfile profile) {
		if (GeoDistanceCalculator.distanceMeter(from, to) < 1.0) {
			return new RouteLegResponse(
				sequence,
				TransportMode.WALK,
				walkRole(sequence, odsayLeg),
				walkInstruction(sequence, odsayLeg),
				BigDecimal.ZERO.setScale(2),
				0,
				0,
				lineString(from, to),
				List.of());
		}
		try {
			GraphHopperRoutePath path = graphHopperRouteClient.route(new GraphHopperRouteRequest(
				from,
				to,
				walkRouteProfileService.resolve(
					profile.primaryUserType(),
					profile.mobilitySubtype(),
					RouteOption.SAFE)));
			return walkRoutePayloadService.toWalkLeg(
				sequence,
				walkRole(sequence, odsayLeg),
				walkInstruction(sequence, odsayLeg),
				path);
		} catch (RouteException exception) {
			if (exception.getErrorCode() == RouteErrorCode.ROUTE_NOT_FOUND) {
				return null;
			}
			throw exception;
		}
	}

	private RouteLegResponse toTransitLeg(int sequence, OdsayTransitLeg odsayLeg, String geometry) {
		int durationSecond = Math.max(0, odsayLeg.sectionTimeMinute() * 60);
		List<TransitLaneOptionResponse> laneOptions = laneOptions(odsayLeg);
		return new RouteLegResponse(
			sequence,
			odsayLeg.type(),
			RouteLegRole.TRANSIT,
			instruction(odsayLeg),
			scale(odsayLeg.distanceMeter()),
			durationSecond,
			estimatedMinute(durationSecond),
			geometry != null ? geometry
				: lineString(odsayLeg.startLng(), odsayLeg.startLat(), odsayLeg.endLng(), odsayLeg.endLat()),
			List.of(),
			routeNo(odsayLeg),
			laneOptions,
			stop(odsayLeg.startName(), odsayLeg.startLat(), odsayLeg.startLng()),
			stop(odsayLeg.endName(), odsayLeg.endLat(), odsayLeg.endLng()),
			null,
			odsayLeg.type() == TransportMode.SUBWAY ? List.of(RouteBadge.ELEVATOR) : List.of());
	}

	private String nextGeometry(
		TransportMode type,
		List<OdsayLaneGeometry> laneGeometries,
		int[] geometryIndex) {
		for (int index = geometryIndex[0]; index < laneGeometries.size(); index++) {
			OdsayLaneGeometry geometry = laneGeometries.get(index);
			geometryIndex[0] = index + 1;
			if (geometry.type() == type) {
				return geometry.geometry();
			}
		}
		return null;
	}

	private RouteLegRole walkRole(int sequence, OdsayTransitLeg odsayLeg) {
		if (sequence == 1 || odsayLeg.sectionTimeMinute() == 0) {
			return RouteLegRole.WALK_TO_TRANSIT;
		}
		return RouteLegRole.WALK_TO_DESTINATION;
	}

	private String walkInstruction(int sequence, OdsayTransitLeg odsayLeg) {
		if (walkRole(sequence, odsayLeg) == RouteLegRole.WALK_TO_DESTINATION) {
			return "목적지까지 이동하세요.";
		}
		return "대중교통 탑승 지점까지 이동하세요.";
	}

	private List<TransitLaneOptionResponse> laneOptions(OdsayTransitLeg odsayLeg) {
		if (odsayLeg.type() != TransportMode.BUS) {
			return List.of();
		}
		int durationSecond = Math.max(0, odsayLeg.sectionTimeMinute() * 60);
		return odsayLeg.lanes()
			.stream()
			.map(lane -> new TransitLaneOptionResponse(
				lane.busNo(),
				null,
				durationSecond,
				estimatedMinute(durationSecond),
				null))
			.toList();
	}

	private RouteStopResponse stop(String name, BigDecimal lat, BigDecimal lng) {
		if (lat == null || lng == null) {
			return null;
		}
		return new RouteStopResponse(name, lat, lng);
	}

	private String instruction(OdsayTransitLeg leg) {
		if (leg.type() == TransportMode.BUS) {
			return routeNo(leg) + "번 버스를 탑승하세요.";
		}
		return routeNo(leg) + "을/를 탑승하세요.";
	}

	private String routeNo(OdsayTransitLeg leg) {
		return leg.lanes()
			.stream()
			.map(lane -> leg.type() == TransportMode.BUS ? lane.busNo() : lane.name())
			.filter(value -> value != null && !value.isBlank())
			.findFirst()
			.orElse(leg.type().name());
	}

	private String title(List<OdsayTransitLeg> legs) {
		List<String> routeNumbers = legs.stream()
			.filter(leg -> leg.type() == TransportMode.BUS || leg.type() == TransportMode.SUBWAY)
			.map(this::routeNo)
			.distinct()
			.toList();
		return routeNumbers.isEmpty() ? "대중교통 경로" : String.join(" / ", routeNumbers) + " 경로";
	}

	private int transferCount(OdsayTransitPath path) {
		return Math.max(0, path.busTransitCount() + path.subwayTransitCount() - 1);
	}

	private String mergeGeometry(List<RouteLegResponse> legs) {
		List<String> coordinates = new ArrayList<>();
		for (RouteLegResponse leg : legs) {
			coordinates.addAll(extractCoordinates(leg.geometry()));
		}
		if (coordinates.size() < 2) {
			return null;
		}
		List<String> merged = new ArrayList<>();
		for (String coordinate : coordinates) {
			if (merged.isEmpty() || !merged.get(merged.size() - 1).equals(coordinate)) {
				merged.add(coordinate);
			}
		}
		return "LINESTRING(" + String.join(", ", merged) + ")";
	}

	private List<String> extractCoordinates(String lineString) {
		if (lineString == null || !lineString.startsWith("LINESTRING(")) {
			return List.of();
		}
		return List.of(lineString.substring("LINESTRING(".length(), lineString.length() - 1).split(", "));
	}

	private String lineString(GeoPointRequest from, GeoPointRequest to) {
		return lineString(
			BigDecimal.valueOf(from.lng()),
			BigDecimal.valueOf(from.lat()),
			BigDecimal.valueOf(to.lng()),
			BigDecimal.valueOf(to.lat()));
	}

	private String lineString(BigDecimal startLng, BigDecimal startLat, BigDecimal endLng, BigDecimal endLat) {
		if (startLng == null || startLat == null || endLng == null || endLat == null) {
			return null;
		}
		return "LINESTRING(%s %s, %s %s)".formatted(startLng, startLat, endLng, endLat);
	}

	private BigDecimal scale(BigDecimal value) {
		if (value == null) {
			return null;
		}
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private int estimatedMinute(int durationSecond) {
		if (durationSecond <= 0) {
			return 0;
		}
		return Math.max(1, (int)Math.ceil(durationSecond / 60.0));
	}

	private List<Map<String, Object>> snapshotLegs(OdsayTransitPath path) {
		return path.legs()
			.stream()
			.map(this::snapshotLeg)
			.toList();
	}

	private Map<String, Object> snapshotLeg(OdsayTransitLeg leg) {
		Map<String, Object> snapshot = new LinkedHashMap<>(leg.snapshot());
		snapshot.put("type", leg.type());
		snapshot.put("lanes", leg.lanes().stream().map(this::snapshotLane).toList());
		snapshot.put("passStops", leg.passStops().stream().map(this::snapshotStop).toList());
		return snapshot;
	}

	private Map<String, Object> snapshotLane(OdsayTransitLane lane) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("busNo", lane.busNo());
		snapshot.put("busLocalBlID", lane.busLocalBlId());
		snapshot.put("subwayCode", lane.subwayCode());
		snapshot.put("name", lane.name());
		return snapshot;
	}

	private Map<String, Object> snapshotStop(OdsayPassStop stop) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("stationID", stop.stationId());
		snapshot.put("stationName", stop.stationName());
		snapshot.put("localStationID", stop.localStationId());
		snapshot.put("arsID", stop.arsId());
		snapshot.put("lat", stop.lat());
		snapshot.put("lng", stop.lng());
		return snapshot;
	}

	private void validateServiceArea(GeoPointRequest startPoint, GeoPointRequest endPoint) {
		if (!isInBusan(startPoint) || !isInBusan(endPoint)) {
			throw new RouteException(RouteErrorCode.OUT_OF_SERVICE_AREA);
		}
	}

	private boolean isInBusan(GeoPointRequest point) {
		return point.lat() >= BUSAN_MIN_LAT
			&& point.lat() <= BUSAN_MAX_LAT
			&& point.lng() >= BUSAN_MIN_LNG
			&& point.lng() <= BUSAN_MAX_LNG;
	}

	private void validateStartEndDistance(GeoPointRequest startPoint, GeoPointRequest endPoint) {
		if (GeoDistanceCalculator.distanceMeter(startPoint, endPoint) <= START_END_MIN_DISTANCE_METER) {
			throw new RouteException(RouteErrorCode.START_END_TOO_CLOSE);
		}
	}
}
