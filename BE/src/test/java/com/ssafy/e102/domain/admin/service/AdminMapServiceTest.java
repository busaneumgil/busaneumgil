package com.ssafy.e102.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.List;

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

import com.ssafy.e102.domain.admin.dto.response.AdminAreaListResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminFacilityPayloadResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkResponse;
import com.ssafy.e102.domain.admin.repository.AdminAreaRepository;
import com.ssafy.e102.domain.place.entity.Place;
import com.ssafy.e102.domain.place.repository.PlaceRepository;
import com.ssafy.e102.domain.place.type.PlaceCategory;
import com.ssafy.e102.domain.route.entity.RoadSegment;
import com.ssafy.e102.domain.route.repository.RoadSegmentRepository;

@ExtendWith(MockitoExtension.class)
class AdminMapServiceTest {

	private static final Sort PLACE_SORT = Sort.by(Sort.Direction.ASC, "placeId");

	@Mock
	private AdminAreaRepository adminAreaRepository;

	@Mock
	private RoadSegmentRepository roadSegmentRepository;

	@Mock
	private PlaceRepository placeRepository;

	private AdminMapService adminMapService;
	private GeometryFactory geometryFactory;

	@BeforeEach
	void setUp() {
		adminMapService = new AdminMapService(adminAreaRepository, roadSegmentRepository, placeRepository);
		geometryFactory = new GeometryFactory();
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

		AdminFacilityPayloadResponse response = adminMapService.getFacilities(10);

		assertThat(response.summary().facilityCount()).isEqualTo(1);
		assertThat(response.facilities().features()).hasSize(1);
		assertThat(response.facilities().features().get(0).properties().category())
			.isEqualTo(PlaceCategory.PUBLIC_OFFICE);
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
}
