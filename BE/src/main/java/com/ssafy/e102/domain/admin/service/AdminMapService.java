package com.ssafy.e102.domain.admin.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.admin.dto.request.AdminPlaceAccessibilityFeaturesUpdateRequest;
import com.ssafy.e102.domain.admin.dto.request.AdminPlaceUpdateRequest;
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
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkSummaryResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadSegmentPropertiesResponse;
import com.ssafy.e102.domain.admin.repository.AdminAreaRepository;
import com.ssafy.e102.domain.place.entity.Place;
import com.ssafy.e102.domain.place.entity.PlaceAccessibilityFeature;
import com.ssafy.e102.domain.place.exception.PlaceErrorCode;
import com.ssafy.e102.domain.place.exception.PlaceException;
import com.ssafy.e102.domain.place.repository.PlaceAccessibilityFeatureRepository;
import com.ssafy.e102.domain.place.repository.PlaceRepository;
import com.ssafy.e102.domain.place.type.AccessibilityFeatureType;
import com.ssafy.e102.domain.route.entity.RoadSegment;
import com.ssafy.e102.domain.route.repository.RoadSegmentRepository;
import com.ssafy.e102.global.geo.GeoPointConverter;

@Service
@Transactional(readOnly = true)
public class AdminMapService {

	private static final Sort ROAD_SEGMENT_SORT = Sort.by(Sort.Direction.ASC, "edgeId");
	private static final Sort PLACE_SORT = Sort.by(Sort.Direction.ASC, "placeId");

	private final AdminAreaRepository adminAreaRepository;
	private final RoadSegmentRepository roadSegmentRepository;
	private final PlaceRepository placeRepository;
	private final PlaceAccessibilityFeatureRepository placeAccessibilityFeatureRepository;
	private final GeoPointConverter geoPointConverter;
	private final AdminService adminService;

	public AdminMapService(
		AdminAreaRepository adminAreaRepository,
		RoadSegmentRepository roadSegmentRepository,
		PlaceRepository placeRepository,
		PlaceAccessibilityFeatureRepository placeAccessibilityFeatureRepository,
		GeoPointConverter geoPointConverter,
		AdminService adminService) {
		this.adminAreaRepository = adminAreaRepository;
		this.roadSegmentRepository = roadSegmentRepository;
		this.placeRepository = placeRepository;
		this.placeAccessibilityFeatureRepository = placeAccessibilityFeatureRepository;
		this.geoPointConverter = geoPointConverter;
		this.adminService = adminService;
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
			roadSegments = roadSegmentRepository.findAllIntersectingArea(gu, dong, limit);
			segmentCount = roadSegmentRepository.countIntersectingArea(gu, dong);
		} else {
			Page<RoadSegment> page = roadSegmentRepository.findAll(PageRequest.of(0, limit, ROAD_SEGMENT_SORT));
			roadSegments = page.getContent();
			segmentCount = roadSegmentRepository.count();
		}

		List<AdminGeoJsonFeatureResponse<AdminLineStringGeometryResponse, AdminRoadSegmentPropertiesResponse>> features = roadSegments
			.stream()
			.map(this::toRoadSegmentFeature)
			.toList();

		return new AdminRoadNetworkResponse(
			new AdminRoadNetworkSummaryResponse(segmentCount, features.size()),
			toBbox(roadSegments.stream()
				.map(RoadSegment::getGeom)
				.map(LineString::getEnvelopeInternal)
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

	@Transactional
	public AdminPlaceDetailResponse updatePlace(
		UUID userId,
		Long placeId,
		String gu,
		String dong,
		AdminPlaceUpdateRequest request) {
		validateEditablePlace(userId, placeId, gu, dong);
		Place place = getPlaceWithAccessibilityFeatures(placeId);
		validateProviderPlaceIdOwner(placeId, normalizeNullableText(request.providerPlaceId()));
		place.updateBasicInfo(
			request.name(),
			request.category(),
			request.address(),
			request.point() == null ? null : geoPointConverter.toPoint(request.point()),
			request.providerPlaceId());
		return AdminPlaceDetailResponse.of(place, geoPointConverter);
	}

	@Transactional
	public AdminPlaceDetailResponse updatePlaceAccessibilityFeatures(
		UUID userId,
		Long placeId,
		String gu,
		String dong,
		AdminPlaceAccessibilityFeaturesUpdateRequest request) {
		validateEditablePlace(userId, placeId, gu, dong);
		Place place = requirePlace(placeId);
		validateUniqueFeatureTypes(request.features());
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
		return AdminPlaceDetailResponse.of(place, savedFeatures, geoPointConverter);
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
		adminService.requireCanEditArea(userId, gu, dong);
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

	private String normalizeNullableText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String toSyntheticDong(String dong) {
		return dong.replace("1동", "동")
			.replace("2동", "동")
			.replace("3동", "동")
			.replace("4동", "동");
	}

	private AdminGeoJsonFeatureResponse<AdminLineStringGeometryResponse, AdminRoadSegmentPropertiesResponse> toRoadSegmentFeature(
		RoadSegment roadSegment) {
		return AdminGeoJsonFeatureResponse.of(
			AdminLineStringGeometryResponse.of(toCoordinates(roadSegment.getGeom())),
			new AdminRoadSegmentPropertiesResponse(
				roadSegment.getEdgeId(),
				roadSegment.getFromNodeId(),
				roadSegment.getToNodeId(),
				roadSegment.getSegmentType(),
				roadSegment.getLengthMeter(),
				roadSegment.getWalkAccess(),
				roadSegment.getBrailleBlockState(),
				roadSegment.getAudioSignalState(),
				roadSegment.getWidthState(),
				roadSegment.getSurfaceState(),
				roadSegment.getStairsState(),
				roadSegment.getSignalState()));
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
}
