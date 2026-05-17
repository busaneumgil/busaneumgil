package com.ssafy.e102.domain.admin.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.ssafy.e102.domain.admin.dto.request.AdminPlaceAccessibilityFeaturesUpdateRequest;
import com.ssafy.e102.domain.admin.dto.request.AdminPlaceUpdateRequest;
import com.ssafy.e102.domain.admin.dto.request.AdminRoadSegmentAttributesUpdateRequest;
import com.ssafy.e102.domain.admin.dto.response.AdminAreaListResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminAreaResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminFacilityPayloadResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminFacilityPayloadResponse.AdminFacilitySummaryResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminFacilityPropertiesResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminGeoJsonFeatureCollectionResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminGeoJsonFeatureResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminLineStringGeometryResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminPlaceDetailResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminPointGeometryResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkBridgePayloadResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkBridgePropertiesResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkBridgeSummaryResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkSummaryResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNodePropertiesResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadSegmentPropertiesResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadSegmentUpdateResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoutingPatchStatus;
import com.ssafy.e102.domain.admin.repository.AdminAreaRepository;
import com.ssafy.e102.domain.admin.type.AdminAreaAssignmentType;
import com.ssafy.e102.domain.place.entity.Place;
import com.ssafy.e102.domain.place.entity.PlaceAccessibilityFeature;
import com.ssafy.e102.domain.place.exception.PlaceErrorCode;
import com.ssafy.e102.domain.place.exception.PlaceException;
import com.ssafy.e102.domain.place.repository.PlaceAccessibilityFeatureRepository;
import com.ssafy.e102.domain.place.repository.PlaceRepository;
import com.ssafy.e102.domain.place.type.AccessibilityFeatureType;
import com.ssafy.e102.domain.route.entity.RoadNode;
import com.ssafy.e102.domain.route.entity.RoadSegment;
import com.ssafy.e102.domain.route.entity.SegmentFeature;
import com.ssafy.e102.domain.route.repository.RoadNodeRepository;
import com.ssafy.e102.domain.route.repository.RoadSegmentRepository;
import com.ssafy.e102.domain.route.repository.SegmentFeatureRepository;
import com.ssafy.e102.domain.route.type.AccessibilityState;
import com.ssafy.e102.domain.route.type.SegmentFeatureType;
import com.ssafy.e102.domain.route.type.SegmentType;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient.GraphHopperPatchResult;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient.GraphHopperPatchStatus;
import com.ssafy.e102.global.exception.BusinessException;
import com.ssafy.e102.global.exception.CommonErrorCode;
import com.ssafy.e102.global.geo.GeoPointConverter;

@Service
@Transactional(readOnly = true)
public class AdminMapService {

	private static final Sort ROAD_SEGMENT_SORT = Sort.by(Sort.Direction.ASC, "edgeId");
	private static final Sort PLACE_SORT = Sort.by(Sort.Direction.ASC, "placeId");
	private static final double BRIDGE_MAX_DISTANCE_METER = 50.0;
	private static final double BRIDGE_AUTO_DISTANCE_METER = 12.0;

	private final AdminAreaRepository adminAreaRepository;
	private final RoadNodeRepository roadNodeRepository;
	private final RoadSegmentRepository roadSegmentRepository;
	private final SegmentFeatureRepository segmentFeatureRepository;
	private final PlaceRepository placeRepository;
	private final PlaceAccessibilityFeatureRepository placeAccessibilityFeatureRepository;
	private final GeoPointConverter geoPointConverter;
	private final AdminService adminService;
	private final AdminAuditLogService adminAuditLogService;
	private final GraphHopperAdminClient graphHopperAdminClient;
	private final TransactionTemplate transactionTemplate;

	public AdminMapService(
		AdminAreaRepository adminAreaRepository,
		RoadNodeRepository roadNodeRepository,
		RoadSegmentRepository roadSegmentRepository,
		SegmentFeatureRepository segmentFeatureRepository,
		PlaceRepository placeRepository,
		PlaceAccessibilityFeatureRepository placeAccessibilityFeatureRepository,
		GeoPointConverter geoPointConverter,
		AdminService adminService,
		AdminAuditLogService adminAuditLogService,
		GraphHopperAdminClient graphHopperAdminClient,
		PlatformTransactionManager transactionManager) {
		this.adminAreaRepository = adminAreaRepository;
		this.roadNodeRepository = roadNodeRepository;
		this.roadSegmentRepository = roadSegmentRepository;
		this.segmentFeatureRepository = segmentFeatureRepository;
		this.placeRepository = placeRepository;
		this.placeAccessibilityFeatureRepository = placeAccessibilityFeatureRepository;
		this.geoPointConverter = geoPointConverter;
		this.adminService = adminService;
		this.adminAuditLogService = adminAuditLogService;
		this.graphHopperAdminClient = graphHopperAdminClient;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public AdminAreaListResponse getAreas() {
		Map<String, Set<String>> dongsByGu = new TreeMap<>();
		adminAreaRepository.findDistinctAreas()
			.forEach(row -> {
				String gu = (String)row[0];
				String dong = (String)row[1];
				dongsByGu.computeIfAbsent(gu, key -> new TreeSet<>())
					.add(dong);
			});

		List<AdminAreaResponse> areas = new ArrayList<>();
		dongsByGu.forEach((gu, dongs) -> {
			Set<String> syntheticDongs = new TreeSet<>();
			dongs.forEach(dong -> {
				String syntheticDong = toSyntheticDong(dong);
				if (!syntheticDong.equals(dong) && !dongs.contains(syntheticDong)) {
					syntheticDongs.add(syntheticDong);
				}
			});
			syntheticDongs.forEach(dong -> areas.add(new AdminAreaResponse(gu, dong)));
			dongs.forEach(dong -> areas.add(new AdminAreaResponse(gu, dong)));
		});
		return new AdminAreaListResponse(areas);
	}

	public AdminRoadNetworkResponse getRoadNetwork(String gu, String dong, int limit) {
		List<RoadSegment> roadSegments;
		long segmentCount;
		if (hasArea(gu, dong)) {
			roadSegments = roadSegmentRepository.findAllIntersectingArea(gu, dong);
			segmentCount = roadSegmentRepository.countIntersectingArea(gu, dong);
		} else {
			Page<RoadSegment> page = roadSegmentRepository.findAll(PageRequest.of(0, limit, ROAD_SEGMENT_SORT));
			roadSegments = page.getContent();
			segmentCount = roadSegmentRepository.count();
		}

		Map<Long, List<SegmentFeatureType>> featureTypesByEdgeId = segmentFeatureRepository
			.findByEdgeIdIn(roadSegments.stream()
				.map(RoadSegment::getEdgeId)
				.toList())
			.stream()
			.collect(Collectors.groupingBy(
				SegmentFeature::getEdgeId,
				Collectors.mapping(SegmentFeature::getFeatureType, Collectors.toList())));

		List<AdminGeoJsonFeatureResponse<AdminLineStringGeometryResponse, AdminRoadSegmentPropertiesResponse>> features = roadSegments
			.stream()
			.map(roadSegment -> toRoadSegmentFeature(roadSegment, featureTypesByEdgeId))
			.toList();
		Set<Long> visibleNodeIds = roadSegments.stream()
			.flatMap(roadSegment -> List.of(roadSegment.getFromNodeId(), roadSegment.getToNodeId()).stream())
			.collect(Collectors.toCollection(TreeSet::new));
		List<AdminGeoJsonFeatureResponse<AdminPointGeometryResponse, AdminRoadNodePropertiesResponse>> nodeFeatures = roadNodeRepository
			.findAllById(visibleNodeIds)
			.stream()
			.sorted(Comparator.comparing(RoadNode::getVertexId))
			.map(this::toRoadNodeFeature)
			.toList();

		return new AdminRoadNetworkResponse(
			new AdminRoadNetworkSummaryResponse(segmentCount, features.size(), nodeFeatures.size(), nodeFeatures.size()),
			toBbox(roadSegments.stream()
				.map(RoadSegment::getGeom)
				.map(LineString::getEnvelopeInternal)
				.toList()),
			AdminGeoJsonFeatureCollectionResponse.of(features),
			AdminGeoJsonFeatureCollectionResponse.of(nodeFeatures));
	}

	public AdminRoadNetworkBridgePayloadResponse getRoadNetworkBridges(String gu, String dong) {
		if (!hasArea(gu, dong)) {
			throw new BusinessException(CommonErrorCode.INVALID_INPUT, "구/동은 필수입니다.");
		}
		List<RoadSegment> roadSegments = roadSegmentRepository.findAllIntersectingArea(gu, dong);
		BridgeGraph bridgeGraph = buildBridgeGraph(roadSegments);
		List<BridgeCandidate> candidates = findBridgeCandidates(bridgeGraph);
		List<AdminGeoJsonFeatureResponse<AdminLineStringGeometryResponse, AdminRoadNetworkBridgePropertiesResponse>> features = candidates
			.stream()
			.map(this::toBridgeFeature)
			.toList();

		return new AdminRoadNetworkBridgePayloadResponse(
			new AdminRoadNetworkBridgeSummaryResponse(
				bridgeGraph.componentCount(),
				bridgeGraph.endpointCount(),
				candidates.size(),
				features.size(),
				BRIDGE_MAX_DISTANCE_METER,
				BRIDGE_AUTO_DISTANCE_METER),
			toBbox(features.stream()
				.map(feature -> toEnvelope(feature.geometry().coordinates()))
				.toList()),
			AdminGeoJsonFeatureCollectionResponse.of(features));
	}

	public AdminFacilityPayloadResponse getFacilities(String gu, String dong, int limit) {
		List<Place> places;
		if (hasArea(gu, dong)) {
			places = placeRepository.findAllIntersectingArea(gu, dong, limit);
		} else {
			places = placeRepository.findAll(PageRequest.of(0, limit, PLACE_SORT)).getContent();
		}
		List<AdminGeoJsonFeatureResponse<AdminPointGeometryResponse, AdminFacilityPropertiesResponse>> features = places
			.stream()
			.map(this::toFacilityFeature)
			.toList();
		Map<String, Long> categoryCounts = places.stream()
			.collect(Collectors.groupingBy(
				place -> place.getCategory().name(),
				LinkedHashMap::new,
				Collectors.counting()));

		return new AdminFacilityPayloadResponse(
			new AdminFacilitySummaryResponse(
				features.size(),
				features.size(),
				places.stream()
					.filter(place -> place.getProviderPlaceId() != null && !place.getProviderPlaceId().isBlank())
					.count(),
				sortMap(categoryCounts),
				sortMap(categoryCounts)),
			toBbox(places.stream()
				.map(Place::getPoint)
				.map(Point::getEnvelopeInternal)
				.toList()),
			AdminGeoJsonFeatureCollectionResponse.of(features));
	}

	public AdminPlaceDetailResponse getPlace(Long placeId) {
		Place place = getPlaceWithAccessibilityFeatures(placeId);
		return AdminPlaceDetailResponse.of(place, geoPointConverter);
	}

	public AdminRoadSegmentUpdateResponse updateRoadSegmentAttributes(
		UUID userId,
		Long edgeId,
		String gu,
		String dong,
		AdminRoadSegmentAttributesUpdateRequest request) {
		RoadSegmentUpdateContext updateContext = Objects.requireNonNull(
			transactionTemplate.execute(transactionStatus -> updateRoadSegmentAttributesInTransaction(
				userId,
				edgeId,
				gu,
				dong,
				request)));
		GraphHopperPatchResult routingPatchResult = resolveRoutingPatchResult(edgeId, updateContext);
		return new AdminRoadSegmentUpdateResponse(
			updateContext.after(),
			toAdminRoutingPatchStatus(routingPatchResult.status()),
			routingPatchResult.message());
	}

	@Transactional
	public AdminPlaceDetailResponse updatePlace(
		UUID userId,
		Long placeId,
		String gu,
		String dong,
		AdminPlaceUpdateRequest request) {
		validateEditablePlace(userId, placeId, gu, dong);
		Place place = getPlaceWithAccessibilityFeatures(placeId);
		AdminPlaceDetailResponse before = AdminPlaceDetailResponse.of(place, geoPointConverter);
		validateProviderPlaceIdOwner(placeId, normalizeNullableText(request.providerPlaceId()));
		place.updateBasicInfo(
			request.name(),
			request.category(),
			request.address(),
			request.point() == null ? null : geoPointConverter.toPoint(request.point()),
			request.providerPlaceId());
		AdminPlaceDetailResponse after = AdminPlaceDetailResponse.of(place, geoPointConverter);
		adminAuditLogService.record(
			userId,
			"PLACE_BASIC_UPDATE",
			"PLACE",
			String.valueOf(placeId),
			gu,
			dong,
			"편의시설 기본 정보 변경 placeId=" + placeId,
			before,
			after);
		return after;
	}

	@Transactional
	public AdminPlaceDetailResponse updatePlaceAccessibilityFeatures(
		UUID userId,
		Long placeId,
		String gu,
		String dong,
		AdminPlaceAccessibilityFeaturesUpdateRequest request) {
		validateEditablePlace(userId, placeId, gu, dong);
		Place beforePlace = getPlaceWithAccessibilityFeatures(placeId);
		AdminPlaceDetailResponse before = AdminPlaceDetailResponse.of(beforePlace, geoPointConverter);
		validateUniqueFeatureTypes(request.features());
		if (hasSameAccessibilityFeatures(beforePlace, request.features())) {
			return before;
		}
		Place place = requirePlace(placeId);
		placeAccessibilityFeatureRepository.deleteAllByPlace_PlaceId(placeId);
		placeAccessibilityFeatureRepository.flush();
		List<PlaceAccessibilityFeature> savedFeatures = placeAccessibilityFeatureRepository.saveAll(
			request.features()
				.stream()
				.map(feature -> PlaceAccessibilityFeature.create(
					place,
					feature.featureType(),
					feature.isAvailable()))
				.toList());
		AdminPlaceDetailResponse after = AdminPlaceDetailResponse.of(place, savedFeatures, geoPointConverter);
		adminAuditLogService.record(
			userId,
			"PLACE_ACCESSIBILITY_FEATURES_REPLACE",
			"PLACE",
			String.valueOf(placeId),
			gu,
			dong,
			"편의시설 접근성 속성 교체 placeId=" + placeId,
			before,
			after);
		return after;
	}

	private BridgeGraph buildBridgeGraph(List<RoadSegment> roadSegments) {
		BridgeUnionFind unionFind = new BridgeUnionFind();
		Map<Long, Integer> degreesByNodeId = new HashMap<>();
		Map<Long, BridgeNode> nodesById = new HashMap<>();
		List<GraphSegment> graphSegments = new ArrayList<>();

		for (RoadSegment roadSegment : roadSegments) {
			Long fromNodeId = roadSegment.getFromNodeId();
			Long toNodeId = roadSegment.getToNodeId();
			List<BridgeCoord> coordinates = toBridgeCoords(roadSegment.getGeom());
			if (coordinates.size() < 2) {
				continue;
			}
			BridgeCoord start = coordinates.get(0);
			BridgeCoord end = coordinates.get(coordinates.size() - 1);
			nodesById.putIfAbsent(fromNodeId, new BridgeNode(fromNodeId, start));
			nodesById.putIfAbsent(toNodeId, new BridgeNode(toNodeId, end));
			degreesByNodeId.merge(fromNodeId, 1, Integer::sum);
			degreesByNodeId.merge(toNodeId, 1, Integer::sum);
			unionFind.add(fromNodeId);
			unionFind.add(toNodeId);
			unionFind.union(fromNodeId, toNodeId);
			graphSegments.add(new GraphSegment(
				roadSegment.getEdgeId(),
				fromNodeId,
				toNodeId,
				roadSegment.getSegmentType(),
				coordinates,
				roadSegment.getGeom().getEnvelopeInternal(),
				null));
		}

		Map<Long, Integer> componentIndexes = new HashMap<>();
		List<GraphSegment> segmentsWithComponent = graphSegments.stream()
			.map(segment -> {
				Long root = unionFind.find(segment.fromNodeId());
				Integer componentIndex = componentIndexes.computeIfAbsent(root, ignored -> componentIndexes.size() + 1);
				return segment.withComponentId(componentIndex);
			})
			.toList();
		Map<Long, Integer> componentByNodeId = new HashMap<>();
		nodesById.keySet()
			.forEach(nodeId -> {
				Long root = unionFind.find(nodeId);
				componentByNodeId.put(nodeId,
					componentIndexes.computeIfAbsent(root, ignored -> componentIndexes.size() + 1));
			});
		List<BridgeEndpoint> endpoints = nodesById.values()
			.stream()
			.filter(node -> degreesByNodeId.getOrDefault(node.nodeId(), 0) <= 1)
			.map(node -> new BridgeEndpoint(
				node.nodeId(),
				node.coord(),
				componentByNodeId.getOrDefault(node.nodeId(), 0)))
			.toList();
		return new BridgeGraph(segmentsWithComponent, endpoints, componentIndexes.size());
	}

	private List<BridgeCandidate> findBridgeCandidates(BridgeGraph bridgeGraph) {
		STRtree segmentIndex = new STRtree();
		bridgeGraph.segments().forEach(segment -> segmentIndex.insert(segment.envelope(), segment));
		List<BridgeCandidate> candidates = new ArrayList<>();
		Set<String> candidateKeys = new HashSet<>();

		for (BridgeEndpoint endpoint : bridgeGraph.endpoints()) {
			ClosestBridgeTarget nearest = findNearestBridgeTarget(endpoint, segmentIndex);
			if (nearest == null) {
				continue;
			}
			String key = endpoint.nodeId() + ":" + nearest.segment().edgeId();
			if (!candidateKeys.add(key)) {
				continue;
			}
			String priority = nearest.distanceMeter() <= BRIDGE_AUTO_DISTANCE_METER ? "AUTO" : "REVIEW";
			candidates.add(new BridgeCandidate(
				"bridge-" + (candidates.size() + 1),
				priority,
				endpoint.nodeId(),
				endpoint.componentId(),
				nearest.segment().edgeId(),
				nearest.segment().componentId(),
				roundMeter(nearest.distanceMeter()),
				endpoint.coord(),
				nearest.coord()));
		}

		return candidates.stream()
			.sorted(Comparator
				.comparing(BridgeCandidate::distanceMeter)
				.thenComparing(BridgeCandidate::fromNodeId)
				.thenComparing(BridgeCandidate::toEdgeId))
			.toList();
	}

	@SuppressWarnings("unchecked")
	private ClosestBridgeTarget findNearestBridgeTarget(BridgeEndpoint endpoint, STRtree segmentIndex) {
		Envelope queryEnvelope = new Envelope(
			endpoint.coord().lng(),
			endpoint.coord().lng(),
			endpoint.coord().lat(),
			endpoint.coord().lat());
		queryEnvelope.expandBy(toDegreeExpand(endpoint.coord().lat(), BRIDGE_MAX_DISTANCE_METER));
		List<GraphSegment> nearbySegments = segmentIndex.query(queryEnvelope);

		ClosestBridgeTarget nearest = null;
		for (GraphSegment segment : nearbySegments) {
			if (!isBridgeTargetSegment(segment) || segment.componentId() == endpoint.componentId()) {
				continue;
			}
			ClosestBridgePoint closestPoint = closestPointOnLine(endpoint.coord(), segment.coordinates());
			if (closestPoint.distanceMeter() > BRIDGE_MAX_DISTANCE_METER) {
				continue;
			}
			if (nearest == null || closestPoint.distanceMeter() < nearest.distanceMeter()
				|| closestPoint.distanceMeter() == nearest.distanceMeter()
					&& segment.edgeId() < nearest.segment().edgeId()) {
				nearest = new ClosestBridgeTarget(segment, closestPoint.coord(), closestPoint.distanceMeter());
			}
		}
		return nearest;
	}

	private boolean isBridgeTargetSegment(GraphSegment segment) {
		return segment.segmentType() == SegmentType.SIDE_LINE;
	}

	private AdminGeoJsonFeatureResponse<AdminLineStringGeometryResponse, AdminRoadNetworkBridgePropertiesResponse> toBridgeFeature(
		BridgeCandidate candidate) {
		List<List<Double>> coordinates = List.of(
			List.of(candidate.fromCoord().lng(), candidate.fromCoord().lat()),
			List.of(candidate.toCoord().lng(), candidate.toCoord().lat()));
		return AdminGeoJsonFeatureResponse.of(
			AdminLineStringGeometryResponse.of(coordinates),
			new AdminRoadNetworkBridgePropertiesResponse(
				candidate.candidateId(),
				"PROPOSED_BRIDGE",
				candidate.priority(),
				String.valueOf(candidate.fromNodeId()),
				String.valueOf(candidate.fromComponentId()),
				String.valueOf(candidate.toEdgeId()),
				String.valueOf(candidate.toComponentId()),
				candidate.distanceMeter(),
				List.of(candidate.fromCoord().lng(), candidate.fromCoord().lat()),
				"다른 컴포넌트 endpoint와 가까운 보행 segment 연결 후보"));
	}

	private List<BridgeCoord> toBridgeCoords(LineString lineString) {
		return Arrays.stream(lineString.getCoordinates())
			.map(coordinate -> new BridgeCoord(coordinate.getX(), coordinate.getY()))
			.toList();
	}

	private ClosestBridgePoint closestPointOnLine(BridgeCoord point, List<BridgeCoord> coordinates) {
		ClosestBridgePoint nearest = null;
		for (int i = 1; i < coordinates.size(); i++) {
			ClosestBridgePoint projected = closestPointOnLineSegment(point, coordinates.get(i - 1), coordinates.get(i));
			if (nearest == null || projected.distanceMeter() < nearest.distanceMeter()) {
				nearest = projected;
			}
		}
		return nearest;
	}

	private ClosestBridgePoint closestPointOnLineSegment(BridgeCoord point, BridgeCoord start, BridgeCoord end) {
		double originLat = point.lat();
		LocalMeterCoord pointMeter = toLocalMeters(point, originLat);
		LocalMeterCoord startMeter = toLocalMeters(start, originLat);
		LocalMeterCoord endMeter = toLocalMeters(end, originLat);
		double dx = endMeter.x() - startMeter.x();
		double dy = endMeter.y() - startMeter.y();
		double lengthSquared = dx * dx + dy * dy;
		double t = lengthSquared == 0
			? 0
			: Math.max(0, Math.min(1,
				((pointMeter.x() - startMeter.x()) * dx + (pointMeter.y() - startMeter.y()) * dy) / lengthSquared));
		LocalMeterCoord projectedMeter = new LocalMeterCoord(startMeter.x() + dx * t, startMeter.y() + dy * t);
		double distanceMeter = Math.hypot(pointMeter.x() - projectedMeter.x(), pointMeter.y() - projectedMeter.y());
		return new ClosestBridgePoint(toLngLat(projectedMeter, originLat), distanceMeter);
	}

	private LocalMeterCoord toLocalMeters(BridgeCoord coord, double originLat) {
		double metersPerDegreeLat = 111_320.0;
		double metersPerDegreeLng = 111_320.0 * Math.cos(Math.toRadians(originLat));
		return new LocalMeterCoord(coord.lng() * metersPerDegreeLng, coord.lat() * metersPerDegreeLat);
	}

	private BridgeCoord toLngLat(LocalMeterCoord coord, double originLat) {
		double metersPerDegreeLat = 111_320.0;
		double metersPerDegreeLng = 111_320.0 * Math.cos(Math.toRadians(originLat));
		return new BridgeCoord(coord.x() / metersPerDegreeLng, coord.y() / metersPerDegreeLat);
	}

	private double toDegreeExpand(double lat, double meter) {
		double lngDegree = meter / (111_320.0 * Math.cos(Math.toRadians(lat)));
		double latDegree = meter / 111_320.0;
		return Math.max(lngDegree, latDegree);
	}

	private double roundMeter(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	private Envelope toEnvelope(List<List<Double>> coordinates) {
		Envelope envelope = new Envelope();
		coordinates.forEach(coord -> envelope.expandToInclude(coord.get(0), coord.get(1)));
		return envelope;
	}

	private boolean hasArea(String gu, String dong) {
		return gu != null && !gu.isBlank() && dong != null && !dong.isBlank();
	}

	private Place requirePlace(Long placeId) {
		return placeRepository.findById(placeId)
			.orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND));
	}

	private Place getPlaceWithAccessibilityFeatures(Long placeId) {
		return placeRepository.findWithAccessibilityFeaturesByPlaceId(placeId)
			.orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND));
	}

	private void validateProviderPlaceIdOwner(Long placeId, String providerPlaceId) {
		if (providerPlaceId == null) {
			return;
		}
		placeRepository.findByProviderPlaceId(providerPlaceId)
			.filter(place -> !place.getPlaceId().equals(placeId))
			.ifPresent(ignored -> {
				throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST, "이미 다른 장소에 연결된 providerPlaceId입니다.");
			});
	}

	private void validateEditablePlace(UUID userId, Long placeId, String gu, String dong) {
		adminService.requireCanEditArea(userId, gu, dong, AdminAreaAssignmentType.FACILITY);
		if (!placeRepository.existsIntersectingAreaByPlaceId(placeId, gu, dong)) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST, "담당 구/동의 장소만 수정할 수 있습니다.");
		}
	}

	private void validateUniqueFeatureTypes(
		List<AdminPlaceAccessibilityFeaturesUpdateRequest.Feature> features) {
		Set<AccessibilityFeatureType> featureTypes = EnumSet.noneOf(AccessibilityFeatureType.class);
		for (AdminPlaceAccessibilityFeaturesUpdateRequest.Feature feature : features) {
			if (!featureTypes.add(feature.featureType())) {
				throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST, "접근성 속성 유형은 중복될 수 없습니다.");
			}
		}
	}

	private boolean hasSameAccessibilityFeatures(
		Place place,
		List<AdminPlaceAccessibilityFeaturesUpdateRequest.Feature> requestedFeatures) {
		Map<AccessibilityFeatureType, Boolean> currentFeatures = new EnumMap<>(AccessibilityFeatureType.class);
		place.getAccessibilityFeatures()
			.forEach(feature -> currentFeatures.put(feature.getFeatureType(), feature.isAvailable()));
		Map<AccessibilityFeatureType, Boolean> nextFeatures = new EnumMap<>(AccessibilityFeatureType.class);
		requestedFeatures
			.forEach(feature -> nextFeatures.put(feature.featureType(), Boolean.TRUE.equals(feature.isAvailable())));
		return currentFeatures.equals(nextFeatures);
	}

	private String normalizeNullableText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String toSyntheticDong(String dong) {
		return dong.replace("1\uB3D9", "\uB3D9")
			.replace("2\uB3D9", "\uB3D9")
			.replace("3\uB3D9", "\uB3D9")
			.replace("4\uB3D9", "\uB3D9");
	}

	private RoadSegmentUpdateContext updateRoadSegmentAttributesInTransaction(
		UUID userId,
		Long edgeId,
		String gu,
		String dong,
		AdminRoadSegmentAttributesUpdateRequest request) {
		adminService.requireCanEditArea(userId, gu, dong, AdminAreaAssignmentType.ROAD_NETWORK);
		if (!roadSegmentRepository.existsIntersectingAreaByEdgeId(edgeId, gu, dong)) {
			throw new BusinessException(CommonErrorCode.INVALID_INPUT, "담당 구/동의 segment만 수정할 수 있습니다.");
		}
		RoadSegment roadSegment = roadSegmentRepository.findById(edgeId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "segment를 찾을 수 없습니다."));
		AdminRoadSegmentPropertiesResponse before = toRoadSegmentProperties(roadSegment);
		roadSegment.updateAttributes(
			request.walkAccess(),
			request.brailleBlockState(),
			request.audioSignalState(),
			request.widthState(),
			request.surfaceState(),
			request.stairsState(),
			request.signalState());
		AdminRoadSegmentPropertiesResponse after = toRoadSegmentProperties(roadSegment);
		adminAuditLogService.record(
			userId,
			"ROAD_SEGMENT_ATTRIBUTES_UPDATE",
			"ROAD_SEGMENT",
			String.valueOf(edgeId),
			gu,
			dong,
			"보행 segment 속성 변경 edgeId=" + edgeId,
			before,
			after);
		return new RoadSegmentUpdateContext(before, after, request.walkAccess());
	}

	private GraphHopperPatchResult resolveRoutingPatchResult(Long edgeId, RoadSegmentUpdateContext updateContext) {
		if (updateContext.requestedWalkAccess() == null) {
			return new GraphHopperPatchResult(GraphHopperPatchStatus.SKIPPED, "walk_access update not requested");
		}
		if (updateContext.before().walkAccess() == updateContext.after().walkAccess()) {
			return new GraphHopperPatchResult(GraphHopperPatchStatus.SKIPPED, "walk_access unchanged");
		}
		return graphHopperAdminClient.patchWalkAccess(edgeId, updateContext.after().walkAccess());
	}

	private AdminRoutingPatchStatus toAdminRoutingPatchStatus(GraphHopperPatchStatus patchStatus) {
		return switch (patchStatus) {
			case SKIPPED -> AdminRoutingPatchStatus.SKIPPED;
			case APPLIED -> AdminRoutingPatchStatus.APPLIED;
			case FAILED -> AdminRoutingPatchStatus.FAILED;
		};
	}

	private AdminGeoJsonFeatureResponse<AdminLineStringGeometryResponse, AdminRoadSegmentPropertiesResponse> toRoadSegmentFeature(
		RoadSegment roadSegment,
		Map<Long, List<SegmentFeatureType>> featureTypesByEdgeId) {
		return AdminGeoJsonFeatureResponse.of(
			AdminLineStringGeometryResponse.of(toCoordinates(roadSegment.getGeom())),
			toRoadSegmentProperties(roadSegment,
				featureTypesByEdgeId.getOrDefault(roadSegment.getEdgeId(), List.of())));
	}

	private AdminRoadSegmentPropertiesResponse toRoadSegmentProperties(RoadSegment roadSegment) {
		return toRoadSegmentProperties(roadSegment, List.of());
	}

	private AdminRoadSegmentPropertiesResponse toRoadSegmentProperties(
		RoadSegment roadSegment,
		List<SegmentFeatureType> featureTypes) {
		return new AdminRoadSegmentPropertiesResponse(
			roadSegment.getEdgeId(),
			roadSegment.getFromNodeId(),
			roadSegment.getToNodeId(),
			roadSegment.getSegmentType(),
			roadSegment.getLengthMeter(),
			roadSegment.getAvgSlopePercent(),
			roadSegment.getWidthMeter(),
			roadSegment.getWalkAccess(),
			roadSegment.getBrailleBlockState(),
			roadSegment.getAudioSignalState(),
			roadSegment.getWidthState(),
			roadSegment.getSurfaceState(),
			roadSegment.getStairsState(),
			roadSegment.getSignalState(),
			featureTypes.stream()
				.distinct()
				.sorted(Comparator.comparing(Enum::name))
				.toList());
	}

	private AdminGeoJsonFeatureResponse<AdminPointGeometryResponse, AdminRoadNodePropertiesResponse> toRoadNodeFeature(
		RoadNode roadNode) {
		Point point = roadNode.getPoint();
		return AdminGeoJsonFeatureResponse.of(
			AdminPointGeometryResponse.of(point.getX(), point.getY()),
			new AdminRoadNodePropertiesResponse(
				roadNode.getVertexId(),
				roadNode.getSourceNodeKey()));
	}

	private AdminGeoJsonFeatureResponse<AdminPointGeometryResponse, AdminFacilityPropertiesResponse> toFacilityFeature(
		Place place) {
		Point point = place.getPoint();
		return AdminGeoJsonFeatureResponse.of(
			AdminPointGeometryResponse.of(point.getX(), point.getY()),
			new AdminFacilityPropertiesResponse(
				String.valueOf(place.getPlaceId()),
				place.getName(),
				place.getCategory(),
				place.getAddress() == null ? "" : place.getAddress(),
				place.getProviderPlaceId()));
	}

	private List<List<Double>> toCoordinates(LineString lineString) {
		return Arrays.stream(lineString.getCoordinates())
			.map(coordinate -> List.of(coordinate.getX(), coordinate.getY()))
			.toList();
	}

	private List<Double> toBbox(List<Envelope> envelopes) {
		if (envelopes.isEmpty()) {
			return null;
		}
		Envelope bbox = new Envelope();
		envelopes.forEach(bbox::expandToInclude);
		return List.of(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
	}

	private Map<String, Long> sortMap(Map<String, Long> values) {
		return values.entrySet()
			.stream()
			.sorted(Comparator.comparing(Map.Entry::getKey))
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				Map.Entry::getValue,
				(left, right) -> left,
				LinkedHashMap::new));
	}

	private record BridgeGraph(
		List<GraphSegment> segments,
		List<BridgeEndpoint> endpoints,
		int componentCount) {

		int endpointCount() {
			return endpoints.size();
		}
	}

	private record GraphSegment(
		Long edgeId,
		Long fromNodeId,
		Long toNodeId,
		SegmentType segmentType,
		List<BridgeCoord> coordinates,
		Envelope envelope,
		Integer componentId) {

		GraphSegment withComponentId(Integer nextComponentId) {
			return new GraphSegment(edgeId, fromNodeId, toNodeId, segmentType, coordinates, envelope, nextComponentId);
		}
	}

	private record BridgeNode(
		Long nodeId,
		BridgeCoord coord) {
	}

	private record BridgeEndpoint(
		Long nodeId,
		BridgeCoord coord,
		Integer componentId) {
	}

	private record BridgeCandidate(
		String candidateId,
		String priority,
		Long fromNodeId,
		Integer fromComponentId,
		Long toEdgeId,
		Integer toComponentId,
		Double distanceMeter,
		BridgeCoord fromCoord,
		BridgeCoord toCoord) {
	}

	private record ClosestBridgeTarget(
		GraphSegment segment,
		BridgeCoord coord,
		Double distanceMeter) {
	}

	private record ClosestBridgePoint(
		BridgeCoord coord,
		Double distanceMeter) {
	}

	private record BridgeCoord(
		double lng,
		double lat) {
	}

	private record LocalMeterCoord(
		double x,
		double y) {
	}

	private static final class BridgeUnionFind {

		private final Map<Long, Long> parentByNodeId = new HashMap<>();

		private void add(Long nodeId) {
			parentByNodeId.putIfAbsent(nodeId, nodeId);
		}

		private Long find(Long nodeId) {
			Long parent = parentByNodeId.get(nodeId);
			if (parent == null || parent.equals(nodeId)) {
				return nodeId;
			}
			Long root = find(parent);
			parentByNodeId.put(nodeId, root);
			return root;
		}

		private void union(Long left, Long right) {
			Long leftRoot = find(left);
			Long rightRoot = find(right);
			if (!leftRoot.equals(rightRoot)) {
				parentByNodeId.put(rightRoot, leftRoot);
			}
		}
	}
	private record RoadSegmentUpdateContext(
		AdminRoadSegmentPropertiesResponse before,
		AdminRoadSegmentPropertiesResponse after,
		AccessibilityState requestedWalkAccess) {
	}
}
