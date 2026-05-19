package com.ssafy.e102.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.admin.dto.request.AdminPlaceAccessibilityFeaturesUpdateRequest;
import com.ssafy.e102.domain.admin.dto.request.AdminPlaceUpdateRequest;
import com.ssafy.e102.domain.admin.dto.request.AdminRoadSegmentAttributesUpdateRequest;
import com.ssafy.e102.domain.admin.dto.response.AdminAreaListResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminFacilityPayloadResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminPlaceDetailResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadSegmentUpdateResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoutingApplyStatus;
import com.ssafy.e102.domain.admin.repository.AdminAreaRepository;
import com.ssafy.e102.domain.place.entity.Place;
import com.ssafy.e102.domain.place.entity.PlaceAccessibilityFeature;
import com.ssafy.e102.domain.place.exception.PlaceException;
import com.ssafy.e102.domain.place.repository.PlaceAccessibilityFeatureRepository;
import com.ssafy.e102.domain.place.repository.PlaceRepository;
import com.ssafy.e102.domain.place.type.AccessibilityFeatureType;
import com.ssafy.e102.domain.place.type.PlaceCategory;
import com.ssafy.e102.domain.route.entity.RoadNode;
import com.ssafy.e102.domain.route.entity.RoadSegment;
import com.ssafy.e102.domain.route.entity.RoutingSegmentOverride;
import com.ssafy.e102.domain.report.entity.HazardReportRouteReviewSegmentDraft;
import com.ssafy.e102.domain.route.repository.RoadNodeRepository;
import com.ssafy.e102.domain.route.repository.RoadSegmentRepository;
import com.ssafy.e102.domain.route.repository.RoutingSegmentOverrideRepository;
import com.ssafy.e102.domain.route.repository.SegmentFeatureRepository;
import com.ssafy.e102.domain.route.type.AccessibilityState;
import com.ssafy.e102.domain.route.type.WidthState;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient.GraphHopperReloadResult;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient.GraphHopperReloadStatus;
import com.ssafy.e102.global.geo.GeoPointConverter;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

@ExtendWith(MockitoExtension.class)
class AdminMapServiceTest {

	private static final Sort PLACE_SORT = Sort.by(Sort.Direction.ASC, "placeId");

	@Mock
	private AdminAreaRepository adminAreaRepository;

	@Mock
	private RoadNodeRepository roadNodeRepository;

	@Mock
	private RoadSegmentRepository roadSegmentRepository;

	@Mock
	private SegmentFeatureRepository segmentFeatureRepository;

	@Mock
	private RoutingSegmentOverrideRepository routingSegmentOverrideRepository;

	@Mock
	private PlaceRepository placeRepository;

	@Mock
	private PlaceAccessibilityFeatureRepository placeAccessibilityFeatureRepository;

	@Mock
	private AdminService adminService;

	@Mock
	private AdminAuditLogService adminAuditLogService;

	@Mock
	private GraphHopperAdminClient graphHopperAdminClient;

	private AdminMapService adminMapService;
	private GeometryFactory geometryFactory;
	private GeoPointConverter geoPointConverter;
	private UUID adminUserId;

	@BeforeEach
	void setUp() {
		geoPointConverter = new GeoPointConverter();
		adminMapService = new AdminMapService(
			adminAreaRepository,
			new ObjectMapper(),
			roadNodeRepository,
			roadSegmentRepository,
			segmentFeatureRepository,
			routingSegmentOverrideRepository,
			placeRepository,
			placeAccessibilityFeatureRepository,
			geoPointConverter,
			adminService,
			adminAuditLogService,
			graphHopperAdminClient,
			new NoOpPlatformTransactionManager());
		geometryFactory = new GeometryFactory();
		adminUserId = UUID.randomUUID();
	}

	@Test
	@DisplayName("routing overlay orchestration methods must suspend class-level read-only transactions")
	void routingOverlayOrchestrationMethodsSuspendClassLevelTransaction() throws Exception {
		assertNotSupportedTransaction("updateRoadSegmentAttributes",
			UUID.class,
			Long.class,
			String.class,
			String.class,
			AdminRoadSegmentAttributesUpdateRequest.class);
		assertNotSupportedTransaction("applyRouteReviewSegmentDrafts",
			UUID.class,
			String.class,
			String.class,
			List.class);
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
	@DisplayName("관리자 보행 네트워크는 선택 동 경계와 교차하는 DB segment를 GeoJSON으로 변환한다")
	void getRoadNetwork() {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.findAllIntersectingArea("강서구", "명지동"))
			.thenReturn(List.of(roadSegment));
		when(roadSegmentRepository.countIntersectingArea("강서구", "명지동")).thenReturn(1L);
		when(segmentFeatureRepository.findByEdgeIdIn(List.of(1L))).thenReturn(List.of());
		when(roadNodeRepository.findAllById(any())).thenReturn(List.of(
			roadNode(10L, 129.0, 35.0),
			roadNode(20L, 129.1, 35.1)));

		AdminRoadNetworkResponse response = adminMapService.getRoadNetwork("강서구", "명지동", 10);

		assertThat(response.summary().segmentCount()).isEqualTo(1);
		assertThat(response.segments().features()).hasSize(1);
		assertThat(response.roadNodes().features()).hasSize(2);
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
	@DisplayName("관리자 편의시설은 구/동이 있으면 선택 동 주변 places를 조회한다")
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
	@DisplayName("관리자 segment walk_access 변경에서 즉시 경로 반영이 꺼져 있으면 road_segments만 저장하고 overlay는 건드리지 않는다")
	void updateRoadSegmentAttributesSkipsOverlayWhenImmediateApplyDisabled() {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.existsIntersectingGuByEdgeId(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));

		AdminRoadSegmentUpdateResponse response = adminMapService.updateRoadSegmentAttributes(
			adminUserId,
			1L,
			"강서구",
			"명지동",
			new AdminRoadSegmentAttributesUpdateRequest(AccessibilityState.NO, null, null, null, null, null, null, false));

		assertThat(response.segment().walkAccess()).isEqualTo(AccessibilityState.NO);
		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.SKIPPED);
		assertThat(response.routingApplyMessage()).contains("immediate");
		verify(routingSegmentOverrideRepository, never()).save(any(RoutingSegmentOverride.class));
		verify(routingSegmentOverrideRepository, never()).deleteById(1L);
		verify(graphHopperAdminClient, never()).reloadRoutingOverrides();
	}

	@Test
	@DisplayName("관리자 segment walk_access=NO 변경에서 즉시 경로 반영을 선택하면 overlay upsert 후 runtime reload를 호출한다")
	void updateRoadSegmentAttributesSynchronizesFourColumnOverlayWhenImmediateApplyEnabled() {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.existsIntersectingGuByEdgeId(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));
		when(graphHopperAdminClient.reloadRoutingOverrides())
			.thenReturn(
				new GraphHopperReloadResult(
					GraphHopperReloadStatus.APPLIED,
					"reloaded"));

		AdminRoadSegmentUpdateResponse response = adminMapService.updateRoadSegmentAttributes(
			adminUserId,
			1L,
			"강서구",
			"명지동",
			new AdminRoadSegmentAttributesUpdateRequest(
				AccessibilityState.NO,
				AccessibilityState.YES,
				null,
				WidthState.NARROW,
				null,
				AccessibilityState.YES,
				null,
				true));

		assertThat(response.segment().walkAccess()).isEqualTo(AccessibilityState.NO);
		assertThat(response.segment().brailleBlockState()).isEqualTo(AccessibilityState.YES);
		assertThat(response.segment().widthState()).isEqualTo(WidthState.NARROW);
		assertThat(response.segment().stairsState()).isEqualTo(AccessibilityState.YES);
		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.APPLIED);
		assertThat(response.routingApplyMessage()).isEqualTo("reloaded");
		verify(routingSegmentOverrideRepository).save(org.mockito.ArgumentMatchers.argThat(override ->
			override.getEdgeId().equals(1L)
				&& override.getWalkAccess() == AccessibilityState.NO
				&& override.getBrailleBlockState() == AccessibilityState.YES
				&& override.getWidthState() == WidthState.NARROW
				&& override.getStairsState() == AccessibilityState.YES));
		verify(graphHopperAdminClient).reloadRoutingOverrides();
	}

	@Test
	@DisplayName("관리자 segment 수정에서 overlay 대상 필드 요청이 없으면 기존 overlay와 runtime reload는 생략된다")
	void updateRoadSegmentAttributesSkipsOverlayWhenNoOverlayTargetFieldIsRequested() {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.existsIntersectingGuByEdgeId(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));

		AdminRoadSegmentUpdateResponse response = adminMapService.updateRoadSegmentAttributes(
			adminUserId,
			1L,
			"강서구",
			"명지동",
			new AdminRoadSegmentAttributesUpdateRequest(null, null, null, null, null, null, AccessibilityState.YES, true));

		assertThat(response.segment().signalState()).isEqualTo(AccessibilityState.YES);
		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.SKIPPED);
		assertThat(response.routingApplyMessage()).contains("overlay");
		verify(routingSegmentOverrideRepository, never()).save(any(RoutingSegmentOverride.class));
		verify(routingSegmentOverrideRepository, never()).deleteById(1L);
		verify(graphHopperAdminClient, never()).reloadRoutingOverrides();
	}

	@Test
	@DisplayName("관리자 segment 즉시 반영에서 overlay 대상 필드가 명시 null이면 current-state row를 삭제하고 reload한다")
	void updateRoadSegmentAttributesDeletesOverlayWhenAllOverlayColumnsAreExplicitNull() throws Exception {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.existsIntersectingGuByEdgeId(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));
		when(graphHopperAdminClient.reloadRoutingOverrides())
			.thenReturn(new GraphHopperReloadResult(GraphHopperReloadStatus.APPLIED, "reloaded"));
		AdminRoadSegmentAttributesUpdateRequest request = new ObjectMapper().readValue(
			"""
				{
				  "walkAccess": null,
				  "stairsState": null,
				  "widthState": null,
				  "brailleBlockState": null,
				  "signalState": "YES",
				  "applyRoutingImmediately": true
				}
				""",
			AdminRoadSegmentAttributesUpdateRequest.class);

		AdminRoadSegmentUpdateResponse response = adminMapService.updateRoadSegmentAttributes(
			adminUserId,
			1L,
			"강서구",
			"명지동",
			request);

		assertThat(response.segment().signalState()).isEqualTo(AccessibilityState.YES);
		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.APPLIED);
		assertThat(response.routingApplyMessage()).isEqualTo("reloaded");
		verify(routingSegmentOverrideRepository).deleteById(1L);
		verify(graphHopperAdminClient).reloadRoutingOverrides();
	}

	@Test
	@DisplayName("관리자 segment walk_access=UNKNOWN 변경에서 즉시 경로 반영을 선택하면 stale overlay를 제거하고 runtime reload를 호출한다")
	void updateRoadSegmentAttributesReloadsForStairsState() {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.existsIntersectingGuByEdgeId(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));
		when(graphHopperAdminClient.reloadRoutingOverrides())
			.thenReturn(new GraphHopperReloadResult(GraphHopperReloadStatus.APPLIED, "reloaded"));

		AdminRoadSegmentUpdateResponse response = adminMapService.updateRoadSegmentAttributes(
			adminUserId,
			1L,
			"강서구",
			"명지동",
			new AdminRoadSegmentAttributesUpdateRequest(null, null, null, null, null, AccessibilityState.YES, null, true));

		assertThat(response.segment().stairsState()).isEqualTo(AccessibilityState.YES);
		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.APPLIED);
		verify(routingSegmentOverrideRepository).save(org.mockito.ArgumentMatchers.argThat(override ->
			override.getEdgeId().equals(1L)
				&& override.getWalkAccess() == null
				&& override.getBrailleBlockState() == null
				&& override.getWidthState() == null
				&& override.getStairsState() == AccessibilityState.YES));
		verify(graphHopperAdminClient).reloadRoutingOverrides();
	}

	@Test
	@DisplayName("관리자 segment 수정에서 walk_access 요청이 없으면 overlay와 runtime reload는 생략된다")
	void updateRoadSegmentAttributesReloadsForWidthState() {
		RoadSegment roadSegment = roadSegment(1L);
		RoutingSegmentOverride existingOverride = RoutingSegmentOverride.of(
			1L,
			AccessibilityState.NO,
			null,
			null,
			null);
		when(roadSegmentRepository.existsIntersectingGuByEdgeId(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));
		when(routingSegmentOverrideRepository.findById(1L)).thenReturn(Optional.of(existingOverride));
		when(graphHopperAdminClient.reloadRoutingOverrides())
			.thenReturn(new GraphHopperReloadResult(GraphHopperReloadStatus.APPLIED, "reloaded"));

		AdminRoadSegmentUpdateResponse response = adminMapService.updateRoadSegmentAttributes(
			adminUserId,
			1L,
			"강서구",
			"명지동",
			new AdminRoadSegmentAttributesUpdateRequest(null, null, null, WidthState.NARROW, null, null, null, true));

		assertThat(response.segment().widthState()).isEqualTo(WidthState.NARROW);
		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.APPLIED);
		verify(routingSegmentOverrideRepository).save(org.mockito.ArgumentMatchers.argThat(override ->
			override.getEdgeId().equals(1L)
				&& override.getWalkAccess() == AccessibilityState.NO
				&& override.getBrailleBlockState() == null
				&& override.getWidthState() == WidthState.NARROW
				&& override.getStairsState() == null));
		verify(graphHopperAdminClient).reloadRoutingOverrides();
	}

	@Test
	@DisplayName("관리자 segment braille_block_state=NO 즉시 반영은 current-state 저장과 runtime reload 대상이다")
	void updateRoadSegmentAttributesReloadsForBrailleBlockState() {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.existsIntersectingGuByEdgeId(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));
		when(graphHopperAdminClient.reloadRoutingOverrides())
			.thenReturn(new GraphHopperReloadResult(GraphHopperReloadStatus.APPLIED, "reloaded"));

		AdminRoadSegmentUpdateResponse response = adminMapService.updateRoadSegmentAttributes(
			adminUserId,
			1L,
			"gu",
			"dong",
			new AdminRoadSegmentAttributesUpdateRequest(null, AccessibilityState.NO, null, null, null, null, null, true));

		assertThat(response.segment().brailleBlockState()).isEqualTo(AccessibilityState.NO);
		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.APPLIED);
		verify(routingSegmentOverrideRepository).save(org.mockito.ArgumentMatchers.argThat(override ->
			override.getEdgeId().equals(1L)
				&& override.getWalkAccess() == null
				&& override.getBrailleBlockState() == AccessibilityState.NO
				&& override.getWidthState() == null
				&& override.getStairsState() == null));
		verify(graphHopperAdminClient).reloadRoutingOverrides();
	}

	@Test
	@DisplayName("route review segment drafts are batch-applied and trigger one routing overlay reload")
	void applyRouteReviewSegmentDraftsUpdatesRoadSegmentsAndReloadsOnce() {
		RoadSegment firstSegment = roadSegment(1L);
		RoadSegment secondSegment = roadSegment(2L);
		RoutingSegmentOverride existingOverride = RoutingSegmentOverride.of(
			1L,
			AccessibilityState.NO,
			null,
			null,
			null);
		when(roadSegmentRepository.existsIntersectingAreaByEdgeId(1L, "gu", "dong")).thenReturn(true);
		when(roadSegmentRepository.existsIntersectingAreaByEdgeId(2L, "gu", "dong")).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(firstSegment));
		when(roadSegmentRepository.findById(2L)).thenReturn(Optional.of(secondSegment));
		when(routingSegmentOverrideRepository.findById(1L)).thenReturn(Optional.of(existingOverride));
		when(graphHopperAdminClient.reloadRoutingOverrides())
			.thenReturn(new GraphHopperReloadResult(GraphHopperReloadStatus.APPLIED, "reloaded"));

		GraphHopperReloadResult result = adminMapService.applyRouteReviewSegmentDrafts(
			adminUserId,
			"gu",
			"dong",
			List.of(
				HazardReportRouteReviewSegmentDraft.create(
					1L,
					null,
					null,
					null,
					WidthState.NARROW,
					null,
					null,
					null),
				HazardReportRouteReviewSegmentDraft.create(
					2L,
					AccessibilityState.YES,
					AccessibilityState.NO,
					AccessibilityState.UNKNOWN,
					WidthState.ADEQUATE_120,
					null,
					AccessibilityState.NO,
					AccessibilityState.UNKNOWN)));

		assertThat(result.status()).isEqualTo(GraphHopperReloadStatus.APPLIED);
		assertThat(firstSegment.getWidthState()).isEqualTo(WidthState.NARROW);
		assertThat(secondSegment.getWalkAccess()).isEqualTo(AccessibilityState.YES);
		assertThat(secondSegment.getBrailleBlockState()).isEqualTo(AccessibilityState.NO);
		verify(routingSegmentOverrideRepository).save(org.mockito.ArgumentMatchers.argThat(override ->
			override.getEdgeId().equals(1L)
				&& override.getWalkAccess() == AccessibilityState.NO
				&& override.getWidthState() == WidthState.NARROW));
		verify(routingSegmentOverrideRepository).save(org.mockito.ArgumentMatchers.argThat(override ->
			override.getEdgeId().equals(2L)
				&& override.getWalkAccess() == AccessibilityState.YES
				&& override.getBrailleBlockState() == AccessibilityState.NO
				&& override.getWidthState() == WidthState.ADEQUATE_120
				&& override.getStairsState() == AccessibilityState.NO));
		verify(graphHopperAdminClient, times(1)).reloadRoutingOverrides();
	}

	@Test
	@DisplayName("route review segment drafts without overlay values update road_segments and skip reload")
	void applyRouteReviewSegmentDraftsSkipsReloadWhenNoOverlayValuesExist() {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.existsIntersectingAreaByEdgeId(1L, "gu", "dong")).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));

		GraphHopperReloadResult result = adminMapService.applyRouteReviewSegmentDrafts(
			adminUserId,
			"gu",
			"dong",
			List.of(HazardReportRouteReviewSegmentDraft.create(
				1L,
				null,
				null,
				AccessibilityState.YES,
				null,
				null,
				null,
				AccessibilityState.YES)));

		assertThat(result.status()).isEqualTo(GraphHopperReloadStatus.SKIPPED);
		assertThat(roadSegment.getAudioSignalState()).isEqualTo(AccessibilityState.YES);
		assertThat(roadSegment.getSignalState()).isEqualTo(AccessibilityState.YES);
		verify(routingSegmentOverrideRepository, never()).save(any(RoutingSegmentOverride.class));
		verify(routingSegmentOverrideRepository, never()).deleteById(1L);
		verify(graphHopperAdminClient, never()).reloadRoutingOverrides();
	}

	@Test
	@DisplayName("관리자 장소 기본 정보를 부분 수정한다")
	void updatePlace() throws Exception {
		Place place = place();
		when(placeRepository.existsIntersectingGuByPlaceId(1L, "강서구")).thenReturn(true);
		when(placeRepository.findWithAccessibilityFeaturesByPlaceId(1L)).thenReturn(Optional.of(place));
		when(placeRepository.findByProviderPlaceId("67890")).thenReturn(Optional.empty());

		AdminPlaceDetailResponse response = adminMapService.updatePlace(
			adminUserId,
			1L,
			"강서구",
			"전체",
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
		when(placeRepository.existsIntersectingGuByPlaceId(1L, "강서구")).thenReturn(true);
		when(placeRepository.findWithAccessibilityFeaturesByPlaceId(1L)).thenReturn(Optional.of(place));
		when(placeRepository.findByProviderPlaceId("67890")).thenReturn(Optional.of(otherPlace));

		assertThatThrownBy(() -> adminMapService.updatePlace(
			adminUserId,
			1L,
			"강서구",
			"전체",
			new AdminPlaceUpdateRequest(null, null, null, null, "67890")))
			.isInstanceOf(PlaceException.class);
	}

	@Test
	@DisplayName("관리자 장소 접근성 속성은 요청 목록으로 전체 교체한다")
	void updatePlaceAccessibilityFeatures() throws Exception {
		Place place = place();
		when(placeRepository.existsIntersectingGuByPlaceId(1L, "강서구")).thenReturn(true);
		when(placeRepository.findWithAccessibilityFeaturesByPlaceId(1L)).thenReturn(Optional.of(place));
		when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
		when(placeAccessibilityFeatureRepository.saveAll(any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		AdminPlaceDetailResponse response = adminMapService.updatePlaceAccessibilityFeatures(
			adminUserId,
			1L,
			"강서구",
			"전체",
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
		when(placeRepository.existsIntersectingGuByPlaceId(1L, "강서구")).thenReturn(true);

		assertThatThrownBy(() -> adminMapService.updatePlaceAccessibilityFeatures(
			adminUserId,
			1L,
			"강서구",
			"전체",
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

	private RoadNode roadNode(Long vertexId, double lng, double lat) {
		Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
		point.setSRID(4326);
		return RoadNode.create(vertexId, "node:" + vertexId, point);
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

	private void assertNotSupportedTransaction(String methodName, Class<?>... parameterTypes) throws Exception {
		Transactional transactional = AdminMapService.class
			.getMethod(methodName, parameterTypes)
			.getAnnotation(Transactional.class);

		assertThat(transactional).isNotNull();
		assertThat(transactional.propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
	}

	private static final class NoOpPlatformTransactionManager implements PlatformTransactionManager {

		@Override
		public TransactionStatus getTransaction(TransactionDefinition definition) {
			return new SimpleTransactionStatus();
		}

		@Override
		public void commit(TransactionStatus status) {
		}

		@Override
		public void rollback(TransactionStatus status) {
		}
	}
}
