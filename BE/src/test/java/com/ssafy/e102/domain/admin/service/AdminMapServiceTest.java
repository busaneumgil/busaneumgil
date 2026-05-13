package com.ssafy.e102.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.e102.domain.admin.dto.request.AdminPlaceAccessibilityFeaturesUpdateRequest;
import com.ssafy.e102.domain.admin.dto.request.AdminPlaceUpdateRequest;
import com.ssafy.e102.domain.admin.dto.response.AdminAreaListResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminFacilityPayloadResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminPlaceDetailResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkResponse;
import com.ssafy.e102.domain.admin.repository.AdminAreaRepository;
import com.ssafy.e102.domain.place.entity.Place;
import com.ssafy.e102.domain.place.entity.PlaceAccessibilityFeature;
import com.ssafy.e102.domain.place.exception.PlaceException;
import com.ssafy.e102.domain.place.repository.PlaceAccessibilityFeatureRepository;
import com.ssafy.e102.domain.place.repository.PlaceRepository;
import com.ssafy.e102.domain.place.type.AccessibilityFeatureType;
import com.ssafy.e102.domain.place.type.PlaceCategory;
import com.ssafy.e102.domain.route.entity.RoadSegment;
import com.ssafy.e102.domain.route.repository.RoadSegmentRepository;
import com.ssafy.e102.domain.route.repository.SegmentFeatureRepository;
import com.ssafy.e102.global.geo.GeoPointConverter;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

@ExtendWith(MockitoExtension.class)
class AdminMapServiceTest {

	private static final Sort PLACE_SORT = Sort.by(Sort.Direction.ASC, "placeId");

	@Mock
	private AdminAreaRepository adminAreaRepository;

	@Mock
	private RoadSegmentRepository roadSegmentRepository;

	@Mock
	private SegmentFeatureRepository segmentFeatureRepository;

	@Mock
	private PlaceRepository placeRepository;

	@Mock
	private PlaceAccessibilityFeatureRepository placeAccessibilityFeatureRepository;

	@Mock
	private AdminService adminService;

	@Mock
	private AdminAuditLogService adminAuditLogService;

	private AdminMapService adminMapService;
	private GeometryFactory geometryFactory;
	private GeoPointConverter geoPointConverter;
	private UUID adminUserId;

	@BeforeEach
	void setUp() {
		geoPointConverter = new GeoPointConverter();
		adminMapService = new AdminMapService(
			adminAreaRepository,
			roadSegmentRepository,
			segmentFeatureRepository,
			placeRepository,
			placeAccessibilityFeatureRepository,
			geoPointConverter,
			adminService,
			adminAuditLogService);
		geometryFactory = new GeometryFactory();
		adminUserId = UUID.randomUUID();
	}

	@Test
	@DisplayName("관리자 구/동 목록은 admin_areas의 gu/dong metadata를 반환한다")
	void getAreas() {
		when(adminAreaRepository.findDistinctAreas())
			.thenReturn(List.<Object[]>of(new Object[] {"강서구", "명지동"}));

		AdminAreaListResponse response = adminMapService.getAreas();

		assertThat(response.areas()).hasSize(1);
		assertThat(response.areas().get(0).gu()).isEqualTo("강서구");
		assertThat(response.areas().get(0).dong()).isEqualTo("명지동");
	}

	@Test
	@DisplayName("관리자 보행 네트워크는 행정동 경계와 교차하는 DB segment를 GeoJSON으로 변환한다")
	void getRoadNetwork() {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.findAllIntersectingArea("강서구", "명지동", 10))
			.thenReturn(List.of(roadSegment));
		when(roadSegmentRepository.countIntersectingArea("강서구", "명지동")).thenReturn(1L);
		when(segmentFeatureRepository.findByEdgeIdIn(List.of(1L))).thenReturn(List.of());

		AdminRoadNetworkResponse response = adminMapService.getRoadNetwork("강서구", "명지동", 10);

		assertThat(response.summary().segmentCount()).isEqualTo(1);
		assertThat(response.segments().features()).hasSize(1);
		assertThat(response.segments().features().get(0).geometry().coordinates().get(0))
			.containsExactly(129.0, 35.0);
	}

	@Test
	@DisplayName("관리자 편의시설은 DB places를 GeoJSON으로 변환한다")
	void getFacilities() throws Exception {
		Place place = place();
		PageRequest pageRequest = PageRequest.of(0, 10, PLACE_SORT);
		when(placeRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(place), pageRequest, 1));

		AdminFacilityPayloadResponse response = adminMapService.getFacilities(null, null, 10);

		assertThat(response.summary().facilityCount()).isEqualTo(1);
		assertThat(response.facilities().features()).hasSize(1);
		assertThat(response.facilities().features().get(0).properties().category())
			.isEqualTo(PlaceCategory.PUBLIC_OFFICE);
	}

	@Test
	@DisplayName("관리자 편의시설은 구/동이 있으면 행정동 주변 places를 조회한다")
	void getFacilitiesByArea() throws Exception {
		Place place = place();
		when(placeRepository.findAllIntersectingArea("강서구", "명지동", 10)).thenReturn(List.of(place));

		AdminFacilityPayloadResponse response = adminMapService.getFacilities("강서구", "명지동", 10);

		assertThat(response.summary().facilityCount()).isEqualTo(1);
		assertThat(response.facilities().features()).hasSize(1);
	}

	@Test
	@DisplayName("관리자 장소 상세는 접근성 속성을 함께 반환한다")
	void getPlace() throws Exception {
		Place place = place();
		ReflectionTestUtils.setField(place, "accessibilityFeatures",
			List.of(feature(place, AccessibilityFeatureType.accessibleToilet, true)));
		when(placeRepository.findWithAccessibilityFeaturesByPlaceId(1L)).thenReturn(Optional.of(place));

		AdminPlaceDetailResponse response = adminMapService.getPlace(1L);

		assertThat(response.placeId()).isEqualTo(1L);
		assertThat(response.accessibilityFeatures()).hasSize(1);
		assertThat(response.accessibilityFeatures().get(0).featureType())
			.isEqualTo(AccessibilityFeatureType.accessibleToilet);
	}

	@Test
	@DisplayName("관리자 장소 기본 정보를 부분 수정한다")
	void updatePlace() throws Exception {
		Place place = place();
		when(placeRepository.existsIntersectingAreaByPlaceId(1L, "강서구", "명지동")).thenReturn(true);
		when(placeRepository.findWithAccessibilityFeaturesByPlaceId(1L)).thenReturn(Optional.of(place));
		when(placeRepository.findByProviderPlaceId("67890")).thenReturn(Optional.empty());

		AdminPlaceDetailResponse response = adminMapService.updatePlace(
			adminUserId,
			1L,
			"강서구",
			"명지동",
			new AdminPlaceUpdateRequest(
				" 부산광역시청 ",
				PlaceCategory.PUBLIC_OFFICE,
				"",
				new GeoPointRequest(35.1, 129.1),
				"67890"));

		assertThat(response.name()).isEqualTo("부산광역시청");
		assertThat(response.address()).isNull();
		assertThat(response.providerPlaceId()).isEqualTo("67890");
		assertThat(response.point().lat()).isEqualTo(35.1);
		assertThat(response.point().lng()).isEqualTo(129.1);
	}

	@Test
	@DisplayName("관리자 장소 providerPlaceId는 다른 장소와 중복될 수 없다")
	void updatePlaceDuplicateProviderPlaceId() throws Exception {
		Place place = place();
		Place otherPlace = place();
		ReflectionTestUtils.setField(otherPlace, "placeId", 2L);
		when(placeRepository.existsIntersectingAreaByPlaceId(1L, "강서구", "명지동")).thenReturn(true);
		when(placeRepository.findWithAccessibilityFeaturesByPlaceId(1L)).thenReturn(Optional.of(place));
		when(placeRepository.findByProviderPlaceId("67890")).thenReturn(Optional.of(otherPlace));

		assertThatThrownBy(() -> adminMapService.updatePlace(
			adminUserId,
			1L,
			"강서구",
			"명지동",
			new AdminPlaceUpdateRequest(null, null, null, null, "67890")))
			.isInstanceOf(PlaceException.class);
	}

	@Test
	@DisplayName("관리자 장소 접근성 속성은 요청 목록으로 전체 교체한다")
	void updatePlaceAccessibilityFeatures() throws Exception {
		Place place = place();
		when(placeRepository.existsIntersectingAreaByPlaceId(1L, "강서구", "명지동")).thenReturn(true);
		when(placeRepository.findWithAccessibilityFeaturesByPlaceId(1L)).thenReturn(Optional.of(place));
		when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
		when(placeAccessibilityFeatureRepository.saveAll(any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		AdminPlaceDetailResponse response = adminMapService.updatePlaceAccessibilityFeatures(
			adminUserId,
			1L,
			"강서구",
			"명지동",
			new AdminPlaceAccessibilityFeaturesUpdateRequest(List.of(
				new AdminPlaceAccessibilityFeaturesUpdateRequest.Feature(AccessibilityFeatureType.elevator, true),
				new AdminPlaceAccessibilityFeaturesUpdateRequest.Feature(AccessibilityFeatureType.accessibleToilet,
					false))));

		verify(placeAccessibilityFeatureRepository).deleteAllByPlace_PlaceId(1L);
		verify(placeAccessibilityFeatureRepository).flush();
		assertThat(response.accessibilityFeatures())
			.extracting("featureType")
			.containsExactly(AccessibilityFeatureType.accessibleToilet, AccessibilityFeatureType.elevator);
	}

	@Test
	@DisplayName("관리자 장소 접근성 속성은 같은 유형을 중복 요청할 수 없다")
	void updatePlaceAccessibilityFeaturesDuplicateType() throws Exception {
		Place place = place();
		when(placeRepository.existsIntersectingAreaByPlaceId(1L, "강서구", "명지동")).thenReturn(true);
		when(placeRepository.findWithAccessibilityFeaturesByPlaceId(1L)).thenReturn(Optional.of(place));
		when(placeRepository.findById(1L)).thenReturn(Optional.of(place));

		assertThatThrownBy(() -> adminMapService.updatePlaceAccessibilityFeatures(
			adminUserId,
			1L,
			"강서구",
			"명지동",
			new AdminPlaceAccessibilityFeaturesUpdateRequest(List.of(
				new AdminPlaceAccessibilityFeaturesUpdateRequest.Feature(AccessibilityFeatureType.elevator, true),
				new AdminPlaceAccessibilityFeaturesUpdateRequest.Feature(AccessibilityFeatureType.elevator, false)))))
			.isInstanceOf(PlaceException.class);
	}

	private RoadSegment roadSegment(Long edgeId) {
		LineString geom = geometryFactory.createLineString(new Coordinate[] {
			new Coordinate(129.0, 35.0),
			new Coordinate(129.1, 35.1)
		});
		geom.setSRID(4326);
		return RoadSegment.create(edgeId, 10L, 20L, geom, BigDecimal.valueOf(12.3));
	}

	private Place place() throws Exception {
		Constructor<Place> constructor = Place.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		Place place = constructor.newInstance();
		Point point = geometryFactory.createPoint(new Coordinate(129.0, 35.0));
		point.setSRID(4326);
		ReflectionTestUtils.setField(place, "placeId", 1L);
		ReflectionTestUtils.setField(place, "name", "부산시청");
		ReflectionTestUtils.setField(place, "category", PlaceCategory.PUBLIC_OFFICE);
		ReflectionTestUtils.setField(place, "address", "부산광역시 연제구 중앙대로 1001");
		ReflectionTestUtils.setField(place, "point", point);
		ReflectionTestUtils.setField(place, "providerPlaceId", "12345");
		return place;
	}

	private PlaceAccessibilityFeature feature(
		Place place,
		AccessibilityFeatureType featureType,
		boolean isAvailable) throws Exception {
		Constructor<PlaceAccessibilityFeature> constructor = PlaceAccessibilityFeature.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		PlaceAccessibilityFeature feature = constructor.newInstance();
		ReflectionTestUtils.setField(feature, "id", 1);
		ReflectionTestUtils.setField(feature, "place", place);
		ReflectionTestUtils.setField(feature, "featureType", featureType);
		ReflectionTestUtils.setField(feature, "isAvailable", isAvailable);
		return feature;
	}
}
