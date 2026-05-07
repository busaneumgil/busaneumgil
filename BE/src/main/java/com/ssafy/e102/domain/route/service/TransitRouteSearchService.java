package com.ssafy.e102.domain.route.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.ssafy.e102.domain.route.dto.request.WalkRouteSearchRequest;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteStopResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.TransitLaneOptionResponse;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.entity.SubwayStation;
import com.ssafy.e102.domain.route.entity.SubwayStationElevator;
import com.ssafy.e102.domain.route.entity.SubwayTimetable;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.SubwayStationElevatorRepository;
import com.ssafy.e102.domain.route.repository.SubwayStationRepository;
import com.ssafy.e102.domain.route.repository.SubwayTimetableRepository;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.SubwayServiceDayType;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.global.external.bims.BusanBimsArrival;
import com.ssafy.e102.global.external.bims.BusanBimsClient;
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

	private static final Logger log = LoggerFactory.getLogger(TransitRouteSearchService.class);

	private static final double BUSAN_MIN_LAT = 34.85;
	private static final double BUSAN_MAX_LAT = 35.45;
	private static final double BUSAN_MIN_LNG = 128.70;
	private static final double BUSAN_MAX_LNG = 129.40;
	private static final double START_END_MIN_DISTANCE_METER = 20.0;
	private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

	private final WalkRouteUserProfileQueryService userProfileQueryService;
	private final SubwayStationElevatorRepository subwayStationElevatorRepository;
	private final SubwayStationRepository subwayStationRepository;
	private final SubwayTimetableRepository subwayTimetableRepository;
	private final WalkRouteProfileService walkRouteProfileService;
	private final WalkRoutePayloadService walkRoutePayloadService;
	private final GraphHopperRouteClient graphHopperRouteClient;
	private final BusanBimsClient busanBimsClient;
	private final OdsayClient odsayClient;
	private final RouteSearchCacheService routeSearchCacheService;

	public TransitRouteSearchService(
		WalkRouteUserProfileQueryService userProfileQueryService,
		SubwayStationElevatorRepository subwayStationElevatorRepository,
		SubwayStationRepository subwayStationRepository,
		SubwayTimetableRepository subwayTimetableRepository,
		WalkRouteProfileService walkRouteProfileService,
		WalkRoutePayloadService walkRoutePayloadService,
		GraphHopperRouteClient graphHopperRouteClient,
		BusanBimsClient busanBimsClient,
		OdsayClient odsayClient,
		RouteSearchCacheService routeSearchCacheService) {
		this.userProfileQueryService = userProfileQueryService;
		this.subwayStationElevatorRepository = subwayStationElevatorRepository;
		this.subwayStationRepository = subwayStationRepository;
		this.subwayTimetableRepository = subwayTimetableRepository;
		this.walkRouteProfileService = walkRouteProfileService;
		this.walkRoutePayloadService = walkRoutePayloadService;
		this.graphHopperRouteClient = graphHopperRouteClient;
		this.busanBimsClient = busanBimsClient;
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
		RouteException firstExternalFailure = null;
		for (int index = 0; index < searchResult.paths().size(); index++) {
			OdsayTransitPath path = searchResult.paths().get(index);
			int routeIndex = index + 1;
			try {
				List<OdsayLaneGeometry> laneGeometries = odsayClient.loadLane(path.mapObj());
				toCandidate(searchId, routeIndex, startPoint, endPoint, path, laneGeometries, profile)
					.ifPresent(candidates::add);
			} catch (RouteException exception) {
				if (exception.getErrorCode() == RouteErrorCode.EXTERNAL_ROUTE_API_TIMEOUT) {
					throw exception;
				}
				if (exception.getErrorCode() == RouteErrorCode.EXTERNAL_ROUTE_API_FAILED
					&& firstExternalFailure == null) {
					firstExternalFailure = exception;
				}
				log.warn(
					"transit candidate skipped provider={} operation={} routeIndex={} legIndex={} mapObj={} status={} message={}",
					"route",
					"candidate",
					routeIndex,
					"-",
					path.mapObj(),
					exception.getErrorCode().getStatus(),
					exception.getMessage(),
					exception);
			}
		}
		if (candidates.isEmpty()) {
			if (firstExternalFailure != null) {
				throw firstExternalFailure;
			}
			throw new RouteException(RouteErrorCode.ROUTE_NOT_FOUND);
		}
		List<TransitRouteCandidate> selectedCandidates = selectCandidates(candidates);
		WalkRouteSearchResponse response = new WalkRouteSearchResponse(searchId,
			selectedCandidates.stream().map(TransitRouteCandidate::route).toList());
		routeSearchCacheService.save(response);
		routeSearchCacheService.saveTransitMetadata(searchId,
			selectedCandidates.stream().map(TransitRouteCandidate::snapshot).toList());
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
		List<RouteLegResponse> legs = toLegs(routeIndex, startPoint, endPoint, path.legs(), laneGeometries, profile);
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
		return java.util.Optional.of(new TransitRouteCandidate(
			route,
			new TransitRouteSnapshot(routeId, path.mapObj(), snapshotLegs(path)),
			path.totalWalkMeter()));
	}

	private List<TransitRouteCandidate> selectCandidates(List<TransitRouteCandidate> candidates) {
		Map<String, SelectedTransitRoute> selectedByRoute = new LinkedHashMap<>();
		selectBy(candidates, this::recommendedComparator)
			.ifPresent(candidate -> addSelected(selectedByRoute, candidate, RouteOption.RECOMMENDED));
		selectBy(candidates, this::minTransferComparator)
			.ifPresent(candidate -> addSelected(selectedByRoute, candidate, RouteOption.MIN_TRANSFER));
		selectBy(candidates, this::minWalkComparator)
			.ifPresent(candidate -> addSelected(selectedByRoute, candidate, RouteOption.MIN_WALK));
		if (selectedByRoute.size() < 3) {
			for (TransitRouteCandidate candidate : candidates) {
				addSelected(selectedByRoute, candidate, RouteOption.RECOMMENDED);
				if (selectedByRoute.size() == 3) {
					break;
				}
			}
		}
		return selectedByRoute.values()
			.stream()
			.limit(3)
			.map(SelectedTransitRoute::toCandidate)
			.toList();
	}

	private java.util.Optional<TransitRouteCandidate> selectBy(
		List<TransitRouteCandidate> candidates,
		java.util.function.Supplier<Comparator<TransitRouteCandidate>> comparatorSupplier) {
		return candidates.stream().min(comparatorSupplier.get());
	}

	private Comparator<TransitRouteCandidate> recommendedComparator() {
		return Comparator.comparing(this::hasLowFloorBus).reversed()
			.thenComparing(candidate -> candidate.route().durationSecond())
			.thenComparing(candidate -> candidate.route().transferCount())
			.thenComparing(TransitRouteCandidate::totalWalkMeter);
	}

	private Comparator<TransitRouteCandidate> minTransferComparator() {
		return Comparator.comparingInt((TransitRouteCandidate candidate) -> candidate.route().transferCount())
			.thenComparing(candidate -> candidate.route().durationSecond())
			.thenComparing(TransitRouteCandidate::totalWalkMeter);
	}

	private Comparator<TransitRouteCandidate> minWalkComparator() {
		return Comparator.comparingInt(TransitRouteCandidate::totalWalkMeter)
			.thenComparing(candidate -> candidate.route().durationSecond())
			.thenComparing(candidate -> candidate.route().transferCount());
	}

	private void addSelected(
		Map<String, SelectedTransitRoute> selectedByRoute,
		TransitRouteCandidate candidate,
		RouteOption routeOption) {
		selectedByRoute.computeIfAbsent(routeKey(candidate.route()), key -> new SelectedTransitRoute(candidate))
			.add(routeOption);
	}

	private String routeKey(RouteSummaryResponse route) {
		List<String> transitLegKeys = route.legs()
			.stream()
			.filter(leg -> leg.type() == TransportMode.BUS || leg.type() == TransportMode.SUBWAY)
			.map(leg -> "%s:%s:%s:%s".formatted(
				leg.type(),
				leg.routeNo(),
				stopKey(leg.boardingStop()),
				stopKey(leg.alightingStop())))
			.toList();
		if (transitLegKeys.isEmpty()) {
			return route.routeId();
		}
		return String.join("|", transitLegKeys);
	}

	private String stopKey(RouteStopResponse stop) {
		if (stop == null) {
			return "";
		}
		return "%s:%s:%s".formatted(stop.name(), stop.lat(), stop.lng());
	}

	private boolean hasLowFloorBus(TransitRouteCandidate candidate) {
		return candidate.route()
			.legs()
			.stream()
			.flatMap(leg -> leg.laneOptions().stream())
			.anyMatch(option -> Boolean.TRUE.equals(option.isLowFloor()));
	}

	private record SelectedTransitRoute(
		TransitRouteCandidate candidate,
		Set<RouteOption> routeOptions) {

		private SelectedTransitRoute(TransitRouteCandidate candidate) {
			this(candidate, new LinkedHashSet<>());
		}

		private void add(RouteOption routeOption) {
			routeOptions.add(routeOption);
		}

		private TransitRouteCandidate toCandidate() {
			List<RouteOption> options = List.copyOf(routeOptions);
			RouteSummaryResponse route = candidate.route();
			RouteSummaryResponse routeWithOptions = new RouteSummaryResponse(
				route.routeId(),
				route.transportMode(),
				options.get(0),
				options,
				route.title(),
				route.distanceMeter(),
				route.durationSecond(),
				route.estimatedTimeMinute(),
				route.transferCount(),
				route.badges(),
				route.geometry(),
				route.legs());
			return new TransitRouteCandidate(routeWithOptions, candidate.snapshot(), candidate.totalWalkMeter());
		}
	}

	private List<RouteLegResponse> toLegs(
		int routeIndex,
		GeoPointRequest startPoint,
		GeoPointRequest endPoint,
		List<OdsayTransitLeg> odsayLegs,
		List<OdsayLaneGeometry> laneGeometries,
		WalkRouteUserProfile profile) {
		List<RouteLegResponse> legs = new ArrayList<>();
		GeoPointRequest cursor = startPoint;
		int[] geometryIndex = {0};
		for (OdsayTransitLeg odsayLeg : odsayLegs) {
			int legIndex = legs.size() + 1;
			try {
				if (odsayLeg.type() == TransportMode.WALK) {
					GeoPointRequest nextPoint = nextTransitStart(odsayLegs, odsayLeg, endPoint, cursor);
					RouteLegResponse walkLeg = toWalkLeg(legIndex, cursor, nextPoint, odsayLeg, profile);
					if (walkLeg == null) {
						return List.of();
					}
					legs.add(walkLeg);
					cursor = nextPoint;
					continue;
				}
				RouteLegResponse transitLeg = toTransitLeg(
					legIndex,
					routeIndex,
					odsayLeg,
					cursor,
					nextGeometry(odsayLeg.type(), laneGeometries, geometryIndex));
				legs.add(transitLeg);
				if (transitLeg.alightingStop() != null) {
					cursor = new GeoPointRequest(
						transitLeg.alightingStop().lat().doubleValue(),
						transitLeg.alightingStop().lng().doubleValue());
				}
			} catch (RouteException exception) {
				log.warn(
					"transit leg failed provider={} operation={} routeIndex={} legIndex={} status={} message={}",
					provider(odsayLeg),
					operation(odsayLeg),
					routeIndex,
					legIndex,
					exception.getErrorCode().getStatus(),
					exception.getMessage());
				throw exception;
			}
		}
		return List.copyOf(legs);
	}

	private GeoPointRequest nextTransitStart(
		List<OdsayTransitLeg> odsayLegs,
		OdsayTransitLeg currentLeg,
		GeoPointRequest endPoint,
		GeoPointRequest referencePoint) {
		int currentIndex = odsayLegs.indexOf(currentLeg);
		for (int index = currentIndex + 1; index < odsayLegs.size(); index++) {
			OdsayTransitLeg next = odsayLegs.get(index);
			if (next.type() != TransportMode.WALK) {
				RouteStopResponse stop = boardingStop(next, referencePoint);
				if (stop != null) {
					return new GeoPointRequest(stop.lat().doubleValue(), stop.lng().doubleValue());
				}
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

	private RouteLegResponse toTransitLeg(
		int sequence,
		int routeIndex,
		OdsayTransitLeg odsayLeg,
		GeoPointRequest referencePoint,
		String geometry) {
		int durationSecond = Math.max(0, odsayLeg.sectionTimeMinute() * 60);
		List<TransitLaneOptionResponse> laneOptions = laneOptions(routeIndex, sequence, odsayLeg);
		String routeNo = routeNo(odsayLeg, laneOptions);
		RouteStopResponse boardingStop = boardingStop(odsayLeg, referencePoint);
		RouteStopResponse alightingStop = alightingStop(odsayLeg);
		return new RouteLegResponse(
			sequence,
			odsayLeg.type(),
			RouteLegRole.TRANSIT,
			instruction(odsayLeg, routeNo),
			scale(odsayLeg.distanceMeter()),
			durationSecond,
			estimatedMinute(durationSecond),
			geometry != null ? geometry
				: lineString(odsayLeg.startLng(), odsayLeg.startLat(), odsayLeg.endLng(), odsayLeg.endLat()),
			List.of(),
			routeNo,
			laneOptions,
			boardingStop,
			alightingStop,
			null,
			odsayLeg.type() == TransportMode.SUBWAY ? List.of(RouteBadge.ELEVATOR) : List.of());
	}

	private RouteStopResponse boardingStop(OdsayTransitLeg leg, GeoPointRequest referencePoint) {
		if (leg.type() == TransportMode.SUBWAY) {
			return subwayStop(leg.startName(), leg.startId(), leg.startExitLat(), leg.startExitLng(), referencePoint);
		}
		return stop(leg.startName(), leg.startLat(), leg.startLng());
	}

	private RouteStopResponse alightingStop(OdsayTransitLeg leg) {
		if (leg.type() == TransportMode.SUBWAY) {
			return subwayStop(leg.endName(), leg.endId(), leg.endExitLat(), leg.endExitLng(),
				referencePoint(leg.endLat(), leg.endLng()));
		}
		return stop(leg.endName(), leg.endLat(), leg.endLng());
	}

	private RouteStopResponse subwayStop(String name, String odsayStationId, BigDecimal fallbackLat,
		BigDecimal fallbackLng, GeoPointRequest referencePoint) {
		List<SubwayStationElevator> elevators = subwayStationElevatorRepository.findByOdsayStationId(odsayStationId);
		if (!elevators.isEmpty()) {
			SubwayStationElevator elevator = nearestElevator(elevators, referencePoint);
			return stop(elevator.getStationName() + " 엘리베이터", elevator);
		}
		return subwayStationRepository.findByOdsayStationId(odsayStationId)
			.filter(station -> station.getPoint() != null)
			.map(station -> stop(station.getStationName() + " 엘리베이터", station))
			.orElseGet(() -> stop(name + " 엘리베이터", fallbackLat, fallbackLng));
	}

	private SubwayStationElevator nearestElevator(List<SubwayStationElevator> elevators,
		GeoPointRequest referencePoint) {
		if (referencePoint == null) {
			return elevators.get(0);
		}
		return elevators.stream()
			.min(Comparator.comparingDouble(elevator -> GeoDistanceCalculator.distanceMeter(
				referencePoint,
				new GeoPointRequest(elevator.getPoint().getY(), elevator.getPoint().getX()))))
			.orElse(elevators.get(0));
	}

	private GeoPointRequest referencePoint(BigDecimal lat, BigDecimal lng) {
		if (lat == null || lng == null) {
			return null;
		}
		return new GeoPointRequest(lat.doubleValue(), lng.doubleValue());
	}

	private RouteStopResponse stop(String name, SubwayStation station) {
		return new RouteStopResponse(
			name,
			BigDecimal.valueOf(station.getPoint().getY()),
			BigDecimal.valueOf(station.getPoint().getX()));
	}

	private RouteStopResponse stop(String name, SubwayStationElevator elevator) {
		return new RouteStopResponse(
			name,
			BigDecimal.valueOf(elevator.getPoint().getY()),
			BigDecimal.valueOf(elevator.getPoint().getX()));
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

	private List<TransitLaneOptionResponse> laneOptions(int routeIndex, int legIndex, OdsayTransitLeg odsayLeg) {
		if (odsayLeg.type() != TransportMode.BUS) {
			return List.of();
		}
		int durationSecond = Math.max(0, odsayLeg.sectionTimeMinute() * 60);
		return odsayLeg.lanes()
			.stream()
			.map(lane -> laneOption(routeIndex, legIndex, odsayLeg, lane, durationSecond))
			.sorted((left, right) -> Boolean.compare(
				Boolean.TRUE.equals(right.isLowFloor()),
				Boolean.TRUE.equals(left.isLowFloor())))
			.toList();
	}

	private TransitLaneOptionResponse laneOption(
		int routeIndex,
		int legIndex,
		OdsayTransitLeg odsayLeg,
		OdsayTransitLane lane,
		int durationSecond) {
		String stopId = boardingStopId(odsayLeg);
		try {
			BusanBimsArrival arrival = busanBimsClient.findArrival(stopId, lane.busLocalBlId(), lane.busNo());
			return new TransitLaneOptionResponse(
				lane.busNo(),
				arrival.remainingMinute(),
				durationSecond,
				estimatedMinute(durationSecond),
				arrival.isLowFloor());
		} catch (RouteException exception) {
			log.warn(
				"transit lane failed provider={} operation={} routeIndex={} legIndex={} stopId={} lineId={} routeNo={} status={} message={}",
				"bims",
				"arrival",
				routeIndex,
				legIndex,
				stopId,
				lane.busLocalBlId(),
				lane.busNo(),
				exception.getErrorCode().getStatus(),
				exception.getMessage());
			throw exception;
		}
	}

	private String provider(OdsayTransitLeg odsayLeg) {
		return switch (odsayLeg.type()) {
			case WALK -> "graphhopper";
			case BUS -> "bims";
			case SUBWAY -> "route";
			default -> "route";
		};
	}

	private String operation(OdsayTransitLeg odsayLeg) {
		return switch (odsayLeg.type()) {
			case WALK -> "walkRoute";
			case BUS -> "busArrival";
			case SUBWAY -> "transitLeg";
			default -> "transitLeg";
		};
	}

	private String boardingStopId(OdsayTransitLeg leg) {
		if (!leg.passStops().isEmpty() && leg.passStops().get(0).localStationId() != null) {
			return leg.passStops().get(0).localStationId();
		}
		return leg.startLocalStationId();
	}

	private RouteStopResponse stop(String name, BigDecimal lat, BigDecimal lng) {
		if (lat == null || lng == null) {
			return null;
		}
		return new RouteStopResponse(name, lat, lng);
	}

	private String instruction(OdsayTransitLeg leg, String routeNo) {
		if (leg.type() == TransportMode.BUS) {
			return routeNo + "번 버스를 탑승하세요.";
		}
		return routeNo + "을/를 탑승하세요.";
	}

	private String routeNo(OdsayTransitLeg leg) {
		return leg.lanes()
			.stream()
			.map(lane -> leg.type() == TransportMode.BUS ? lane.busNo() : lane.name())
			.filter(value -> value != null && !value.isBlank())
			.findFirst()
			.orElse(leg.type().name());
	}

	private String routeNo(OdsayTransitLeg leg, List<TransitLaneOptionResponse> laneOptions) {
		if (leg.type() == TransportMode.BUS && !laneOptions.isEmpty()) {
			return laneOptions.get(0).routeNo();
		}
		return routeNo(leg);
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
		if (leg.type() == TransportMode.SUBWAY) {
			RouteStopResponse boardingStop = boardingStop(leg, null);
			RouteStopResponse alightingStop = alightingStop(leg);
			snapshot.put("odsayStationId", leg.startId());
			snapshot.put("endOdsayStationId", leg.endId());
			snapshot.put("lineName", routeNo(leg));
			snapshot.put("wayCode", leg.wayCode());
			snapshot.put("boardingElevator", snapshotStop(boardingStop));
			snapshot.put("alightingElevator", snapshotStop(alightingStop));
			snapshot.put("nextDeparture", nextDepartureSnapshot(leg));
		}
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

	private Map<String, Object> snapshotStop(RouteStopResponse stop) {
		if (stop == null) {
			return Map.of();
		}
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("name", stop.name());
		snapshot.put("lat", stop.lat());
		snapshot.put("lng", stop.lng());
		return snapshot;
	}

	private Map<String, Object> nextDepartureSnapshot(OdsayTransitLeg leg) {
		SubwayTimetable nextDeparture = nextDeparture(leg);
		if (nextDeparture == null) {
			return Map.of();
		}
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("departureTimeText", nextDeparture.getDepartureTimeText());
		snapshot.put("departureSecondOfDay", nextDeparture.getDepartureSecondOfDay());
		snapshot.put("endStationName", nextDeparture.getEndStationName());
		return snapshot;
	}

	private SubwayTimetable nextDeparture(OdsayTransitLeg leg) {
		if (leg.wayCode() == null) {
			return null;
		}
		LocalDateTime now = LocalDateTime.now(SEOUL_ZONE_ID);
		int secondOfDay = now.toLocalTime().toSecondOfDay();
		SubwayServiceDayType serviceDayType = serviceDayType(now.getDayOfWeek());
		List<SubwayTimetable> departures = subwayTimetableRepository.findNextDepartures(
			leg.startId(),
			serviceDayType,
			leg.wayCode(),
			secondOfDay,
			PageRequest.of(0, 1));
		if (!departures.isEmpty()) {
			return departures.get(0);
		}
		List<SubwayTimetable> firstDepartures = subwayTimetableRepository.findFirstDepartures(
			leg.startId(),
			serviceDayType,
			leg.wayCode(),
			PageRequest.of(0, 1));
		return firstDepartures.isEmpty() ? null : firstDepartures.get(0);
	}

	private SubwayServiceDayType serviceDayType(DayOfWeek dayOfWeek) {
		if (dayOfWeek == DayOfWeek.SATURDAY) {
			return SubwayServiceDayType.SATURDAY;
		}
		if (dayOfWeek == DayOfWeek.SUNDAY) {
			return SubwayServiceDayType.HOLIDAY;
		}
		return SubwayServiceDayType.WEEKDAY;
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
