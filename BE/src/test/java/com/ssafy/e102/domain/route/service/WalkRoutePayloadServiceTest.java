package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.RouteStepAlertType;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.domain.route.type.WalkRouteProfile;
import com.ssafy.e102.global.external.graphhopper.GraphHopperCoordinate;
import com.ssafy.e102.global.external.graphhopper.GraphHopperPathDetail;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;

class WalkRoutePayloadServiceTest {

	private final WalkRoutePayloadService service = new WalkRoutePayloadService(new RouteTurnInstructionService());

	@Test
	void mapsGraphHopperPathDetailsToWalkRoutePayload() {
		GraphHopperRoutePath path = new GraphHopperRoutePath(
			new BigDecimal("950.456"),
			960_000,
			List.of(
				new GraphHopperCoordinate(new BigDecimal("128.9360"), new BigDecimal("35.1200")),
				new GraphHopperCoordinate(new BigDecimal("128.9361"), new BigDecimal("35.1201")),
				new GraphHopperCoordinate(new BigDecimal("128.9362"), new BigDecimal("35.1202"))),
			Map.of(
				"segment_type", List.of(new GraphHopperPathDetail(0, 1, "CROSS_WALK")),
				"signal_state", List.of(new GraphHopperPathDetail(0, 1, "YES")),
				"slope_state", List.of(new GraphHopperPathDetail(0, 2, "MODERATE")),
				"avg_slope_percent", List.of(new GraphHopperPathDetail(0, 2, "6.25")),
				"width_state", List.of(new GraphHopperPathDetail(0, 2, "NARROW")),
				"surface_state", List.of(new GraphHopperPathDetail(1, 2, "UNPAVED"))));

		RouteSummaryResponse route = service.toRouteSummary(
			"rs_walk_test",
			new WalkRouteCandidate(RouteOption.SAFE, WalkRouteProfile.PEDESTRIAN_SAFE, path));

		assertThat(route.routeId()).isEqualTo("rs_walk_test_safe");
		assertThat(route.transportMode()).isEqualTo(TransportMode.WALK);
		assertThat(route.routeOption()).isEqualTo(RouteOption.SAFE);
		assertThat(route.distanceMeter()).isEqualByComparingTo("950.46");
		assertThat(route.durationSecond()).isEqualTo(960);
		assertThat(route.estimatedTimeMinute()).isEqualTo(16);
		assertThat(route.badges())
			.containsExactly(
				RouteBadge.MIDDLE_SLOPE,
				RouteBadge.CROSSWALK,
				RouteBadge.NARROW_SIDEWALK,
				RouteBadge.UNPAVED);
		assertThat(route.legs()).hasSize(1);
		assertThat(route.legs().get(0).steps()).hasSize(2);
		assertThat(route.legs().get(0).steps().get(0).instruction()).isEqualTo("직진하세요.");
		assertThat(route.legs().get(0).steps().get(0).alert().type()).isEqualTo(RouteStepAlertType.CROSSWALK);
	}

	@Test
	void splitsStepsByPositionEventDetailAndConnectsTurnAlert() {
		GraphHopperRoutePath path = new GraphHopperRoutePath(
			new BigDecimal("200.00"),
			120_000,
			List.of(
				new GraphHopperCoordinate(new BigDecimal("0.0"), new BigDecimal("0.0")),
				new GraphHopperCoordinate(new BigDecimal("1.0"), new BigDecimal("0.0")),
				new GraphHopperCoordinate(new BigDecimal("1.0"), new BigDecimal("1.0"))),
			Map.of(
				"segment_type", List.of(new GraphHopperPathDetail(0, 1, "CROSS_WALK")),
				"signal_state", List.of(new GraphHopperPathDetail(0, 1, "YES")),
				"width_state", List.of(new GraphHopperPathDetail(0, 2, "ADEQUATE_150"))));

		RouteSummaryResponse route = service.toRouteSummary(
			"rs_walk_test",
			new WalkRouteCandidate(RouteOption.SAFE, WalkRouteProfile.PEDESTRIAN_SAFE, path));

		assertThat(route.legs().get(0).steps()).hasSize(2);
		assertThat(route.legs().get(0).steps().get(0).instruction()).isEqualTo("직진하세요.");
		assertThat(route.legs().get(0).steps().get(0).alert().type()).isEqualTo(RouteStepAlertType.CROSSWALK);
		assertThat(route.legs().get(0).steps().get(0).geometry()).isEqualTo("LINESTRING(0.0 0.0, 1.0 0.0)");
		assertThat(route.legs().get(0).steps().get(1).instruction()).isEqualTo("좌회전하세요.");
		assertThat(route.legs().get(0).steps().get(1).alert()).isNull();
		assertThat(route.legs().get(0).steps().get(1).geometry()).isEqualTo("LINESTRING(1.0 0.0, 1.0 1.0)");
	}
}
