package com.ssafy.e102.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.admin.dto.request.AdminRoadSegmentAttributesUpdateRequest;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadSegmentUpdateResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoutingApplyStatus;
import com.ssafy.e102.domain.admin.repository.AdminAreaRepository;
import com.ssafy.e102.domain.place.repository.PlaceAccessibilityFeatureRepository;
import com.ssafy.e102.domain.place.repository.PlaceRepository;
import com.ssafy.e102.domain.report.entity.HazardReportRouteReviewSegmentDraft;
import com.ssafy.e102.domain.route.entity.RoadSegment;
import com.ssafy.e102.domain.route.entity.RoutingSegmentOverride;
import com.ssafy.e102.domain.route.repository.RoadNodeRepository;
import com.ssafy.e102.domain.route.repository.RoadSegmentRepository;
import com.ssafy.e102.domain.route.repository.RoutingSegmentOverrideRepository;
import com.ssafy.e102.domain.route.repository.SegmentFeatureRepository;
import com.ssafy.e102.domain.route.type.AccessibilityState;
import com.ssafy.e102.domain.route.type.WidthState;
import com.ssafy.e102.global.geo.GeoPointConverter;

@ExtendWith(MockitoExtension.class)
class AdminMapServiceTest {

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
	private AdminRoutingApplyService adminRoutingApplyService;

	private AdminMapService adminMapService;
	private GeometryFactory geometryFactory;
	private UUID adminUserId;

	@BeforeEach
	void setUp() {
		adminMapService = new AdminMapService(
			adminAreaRepository,
			new ObjectMapper(),
			roadNodeRepository,
			roadSegmentRepository,
			segmentFeatureRepository,
			routingSegmentOverrideRepository,
			placeRepository,
			placeAccessibilityFeatureRepository,
			new GeoPointConverter(),
			adminService,
			adminAuditLogService,
			adminRoutingApplyService,
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
	}

	@Test
	@DisplayName("overlay 대상 필드가 저장되면 DB 저장 후 경로 반영 필요 상태를 반환한다")
	void updateRoadSegmentAttributesMarksRoutingApplyPending() {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.existsIntersectingGuByEdgeId(1L, "gu")).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));

		AdminRoadSegmentUpdateResponse response = adminMapService.updateRoadSegmentAttributes(
			adminUserId,
			1L,
			"gu",
			"dong",
			new AdminRoadSegmentAttributesUpdateRequest(AccessibilityState.NO, null, null, null, null, null, null, false));

		assertThat(response.segment().walkAccess()).isEqualTo(AccessibilityState.NO);
		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.PENDING);
		verify(routingSegmentOverrideRepository).save(any(RoutingSegmentOverride.class));
		verify(adminRoutingApplyService).markDirtyInCurrentTransaction();
	}

	@Test
	@DisplayName("overlay 대상 필드가 없으면 dirty를 마킹하지 않는다")
	void updateRoadSegmentAttributesSkipsDirtyWhenNoOverlayTargetFieldExists() {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.existsIntersectingGuByEdgeId(1L, "gu")).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));

		AdminRoadSegmentUpdateResponse response = adminMapService.updateRoadSegmentAttributes(
			adminUserId,
			1L,
			"gu",
			"dong",
			new AdminRoadSegmentAttributesUpdateRequest(null, null, null, null, null, null, AccessibilityState.YES, true));

		assertThat(response.segment().signalState()).isEqualTo(AccessibilityState.YES);
		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.SKIPPED);
		verify(routingSegmentOverrideRepository, never()).save(any(RoutingSegmentOverride.class));
		verify(routingSegmentOverrideRepository, never()).deleteById(1L);
		verify(adminRoutingApplyService, never()).markDirtyInCurrentTransaction();
	}

	@Test
	@DisplayName("overlay 대상 필드가 명시 null이면 override를 삭제하고 dirty를 마킹한다")
	void updateRoadSegmentAttributesDeletesOverrideWhenOverlayColumnsAreExplicitNull() throws Exception {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.existsIntersectingGuByEdgeId(1L, "gu")).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));
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
			"gu",
			"dong",
			request);

		assertThat(response.segment().signalState()).isEqualTo(AccessibilityState.YES);
		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.PENDING);
		verify(routingSegmentOverrideRepository).deleteById(1L);
		verify(adminRoutingApplyService).markDirtyInCurrentTransaction();
	}

	@Test
	@DisplayName("width_state 변경은 기존 override를 병합하고 dirty를 마킹한다")
	void updateRoadSegmentAttributesMergesExistingOverride() {
		RoadSegment roadSegment = roadSegment(1L);
		RoutingSegmentOverride existingOverride = RoutingSegmentOverride.of(1L, AccessibilityState.NO, null, null, null);
		when(roadSegmentRepository.existsIntersectingGuByEdgeId(1L, "gu")).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));
		when(routingSegmentOverrideRepository.findById(1L)).thenReturn(Optional.of(existingOverride));

		AdminRoadSegmentUpdateResponse response = adminMapService.updateRoadSegmentAttributes(
			adminUserId,
			1L,
			"gu",
			"dong",
			new AdminRoadSegmentAttributesUpdateRequest(null, null, null, WidthState.NARROW, null, null, null, true));

		assertThat(response.segment().widthState()).isEqualTo(WidthState.NARROW);
		assertThat(response.routingApplyStatus()).isEqualTo(AdminRoutingApplyStatus.PENDING);
		verify(routingSegmentOverrideRepository).save(any(RoutingSegmentOverride.class));
		verify(adminRoutingApplyService).markDirtyInCurrentTransaction();
	}

	@Test
	@DisplayName("route review segment drafts가 overlay를 포함하면 dirty를 한 번 마킹한다")
	void applyRouteReviewSegmentDraftsMarksDirtyOnce() {
		RoadSegment firstSegment = roadSegment(1L);
		RoadSegment secondSegment = roadSegment(2L);
		RoutingSegmentOverride existingOverride = RoutingSegmentOverride.of(1L, AccessibilityState.NO, null, null, null);
		when(roadSegmentRepository.existsIntersectingAreaByEdgeId(1L, "gu", "dong")).thenReturn(true);
		when(roadSegmentRepository.existsIntersectingAreaByEdgeId(2L, "gu", "dong")).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(firstSegment));
		when(roadSegmentRepository.findById(2L)).thenReturn(Optional.of(secondSegment));
		when(routingSegmentOverrideRepository.findById(1L)).thenReturn(Optional.of(existingOverride));

		boolean result = adminMapService.applyRouteReviewSegmentDraftsInCurrentTransaction(
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

		assertThat(result).isTrue();
		assertThat(firstSegment.getWidthState()).isEqualTo(WidthState.NARROW);
		assertThat(secondSegment.getWalkAccess()).isEqualTo(AccessibilityState.YES);
		verify(adminRoutingApplyService).markDirtyInCurrentTransaction();
	}

	@Test
	@DisplayName("route review segment drafts에 overlay 대상이 없으면 dirty를 마킹하지 않는다")
	void applyRouteReviewSegmentDraftsSkipsDirtyWhenNoOverlayValuesExist() {
		RoadSegment roadSegment = roadSegment(1L);
		when(roadSegmentRepository.existsIntersectingAreaByEdgeId(1L, "gu", "dong")).thenReturn(true);
		when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(roadSegment));

		boolean result = adminMapService.applyRouteReviewSegmentDraftsInCurrentTransaction(
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

		assertThat(result).isFalse();
		assertThat(roadSegment.getAudioSignalState()).isEqualTo(AccessibilityState.YES);
		assertThat(roadSegment.getSignalState()).isEqualTo(AccessibilityState.YES);
		verify(routingSegmentOverrideRepository, never()).save(any(RoutingSegmentOverride.class));
		verify(adminRoutingApplyService, never()).markDirtyInCurrentTransaction();
	}

	private RoadSegment roadSegment(Long edgeId) {
		LineString geom = geometryFactory.createLineString(new Coordinate[] {
			new Coordinate(129.0, 35.0),
			new Coordinate(129.1, 35.1)
		});
		geom.setSRID(4326);
		return RoadSegment.create(edgeId, 10L, 20L, geom, BigDecimal.valueOf(12.3));
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
