package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import com.ssafy.e102.domain.route.dto.response.RouteStepAlertType;

/**
 * route 회전 alert가 geometry 방향 변화에서 파생되는지 검증한다.
 */
class RouteTurnInstructionServiceTest {

	private final GeometryFactory geometryFactory = new GeometryFactory();
	private final RouteTurnInstructionService routeTurnInstructionService = new RouteTurnInstructionService();

	@Test
	void resolveReturnsTurnLeftFromRouteGeometryDirectionChange() {
		LineString routeGeometry = geometryFactory.createLineString(new Coordinate[] {
			new Coordinate(0.0, 0.0),
			new Coordinate(1.0, 0.0),
			new Coordinate(1.0, 1.0)
		});

		Optional<RouteStepAlertType> alertType = routeTurnInstructionService.resolve(routeGeometry, 1);

		assertThat(alertType).contains(RouteStepAlertType.TURN_LEFT);
	}

	@Test
	void resolveReturnsTurnRightFromContinuousSegmentDirectionChange() {
		Optional<RouteStepAlertType> alertType = routeTurnInstructionService.resolve(
			new Coordinate(0.0, 0.0),
			new Coordinate(0.0, 1.0),
			new Coordinate(1.0, 1.0));

		assertThat(alertType).contains(RouteStepAlertType.TURN_RIGHT);
	}

	@Test
	void resolveIgnoresSmallHeadingChange() {
		Optional<RouteStepAlertType> alertType = routeTurnInstructionService.resolve(
			new Coordinate(0.0, 0.0),
			new Coordinate(1.0, 0.0),
			new Coordinate(2.0, 0.2));

		assertThat(alertType).isEmpty();
	}

	@Test
	void resolveDoesNotDependOnSegmentFeatureType() {
		Optional<RouteStepAlertType> alertType = routeTurnInstructionService.resolve(
			new Coordinate(0.0, 0.0),
			new Coordinate(1.0, 0.0),
			new Coordinate(1.0, -1.0));

		assertThat(alertType).contains(RouteStepAlertType.TURN_RIGHT);
	}
}
