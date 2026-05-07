package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import com.ssafy.e102.domain.route.type.RouteStepAlertType;

class RouteTurnInstructionResolverTest {

	private final GeometryFactory geometryFactory = new GeometryFactory();
	private final RouteTurnInstructionResolver resolver = new RouteTurnInstructionResolver();

	@Test
	void resolveReturnsTurnLeftFromRouteGeometryDirectionChange() {
		LineString routeGeometry = geometryFactory.createLineString(new Coordinate[] {
			new Coordinate(0.0, 0.0),
			new Coordinate(1.0, 0.0),
			new Coordinate(1.0, 1.0)
		});

		RouteStepAlertType alertType = resolver.resolve(routeGeometry, 1);

		assertThat(alertType).isEqualTo(RouteStepAlertType.TURN_LEFT);
	}

	@Test
	void resolveReturnsTurnRightFromContinuousSegmentDirectionChange() {
		RouteStepAlertType alertType = resolver.resolve(
			new Coordinate(0.0, 0.0),
			new Coordinate(0.0, 1.0),
			new Coordinate(1.0, 1.0));

		assertThat(alertType).isEqualTo(RouteStepAlertType.TURN_RIGHT);
	}

	@Test
	void resolveIgnoresSmallHeadingChange() {
		RouteStepAlertType alertType = resolver.resolve(
			new Coordinate(0.0, 0.0),
			new Coordinate(1.0, 0.0),
			new Coordinate(2.0, 0.2));

		assertThat(alertType).isEqualTo(RouteStepAlertType.NONE);
	}

	@Test
	void resolveDoesNotDependOnSegmentFeatureType() {
		RouteStepAlertType alertType = resolver.resolve(
			new Coordinate(0.0, 0.0),
			new Coordinate(1.0, 0.0),
			new Coordinate(1.0, -1.0));

		assertThat(alertType).isEqualTo(RouteStepAlertType.TURN_RIGHT);
	}
}
