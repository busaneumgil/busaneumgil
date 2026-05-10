package com.ssafy.e102.domain.admin.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.admin.dto.response.AdminAreaListResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminAreaResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminFacilityPayloadResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminFacilityPayloadResponse.AdminFacilitySummaryResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminFacilityPropertiesResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminGeoJsonFeatureCollectionResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminGeoJsonFeatureResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminLineStringGeometryResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminPointGeometryResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkSummaryResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadSegmentPropertiesResponse;
import com.ssafy.e102.domain.admin.repository.AdminAreaRepository;
import com.ssafy.e102.domain.place.entity.Place;
import com.ssafy.e102.domain.place.repository.PlaceRepository;
import com.ssafy.e102.domain.route.entity.RoadSegment;
import com.ssafy.e102.domain.route.repository.RoadSegmentRepository;

@Service
@Transactional(readOnly = true)
public class AdminMapService {

	private static final Sort ROAD_SEGMENT_SORT = Sort.by(Sort.Direction.ASC, "edgeId");
	private static final Sort PLACE_SORT = Sort.by(Sort.Direction.ASC, "placeId");

	private final AdminAreaRepository adminAreaRepository;
	private final RoadSegmentRepository roadSegmentRepository;
	private final PlaceRepository placeRepository;

	public AdminMapService(
		AdminAreaRepository adminAreaRepository,
		RoadSegmentRepository roadSegmentRepository,
		PlaceRepository placeRepository) {
		this.adminAreaRepository = adminAreaRepository;
		this.roadSegmentRepository = roadSegmentRepository;
		this.placeRepository = placeRepository;
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

	public AdminFacilityPayloadResponse getFacilities(int limit) {
		List<Place> places = placeRepository.findAll(PageRequest.of(0, limit, PLACE_SORT)).getContent();
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

	private boolean hasArea(String gu, String dong) {
		return gu != null && !gu.isBlank() && dong != null && !dong.isBlank();
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
