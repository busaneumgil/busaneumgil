package com.ssafy.e102.domain.route.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import com.ssafy.e102.domain.route.type.SegmentFeatureType;

class SegmentFeatureTest {

	private final GeometryFactory geometryFactory = new GeometryFactory();

	@Test
	void createInitializesFeatureMappingColumns() {
		Point geom = geometryFactory.createPoint(new Coordinate(128.9360, 35.1200));

		SegmentFeature feature = SegmentFeature.create(
			1L,
			100L,
			SegmentFeatureType.WIDTH,
			geom,
			"ADEQUATE_120",
			BigDecimal.valueOf(1.25));

		assertThat(feature.getFeatureId()).isEqualTo(1L);
		assertThat(feature.getEdgeId()).isEqualTo(100L);
		assertThat(feature.getFeatureType()).isEqualTo(SegmentFeatureType.WIDTH);
		assertThat(feature.getGeom()).isSameAs(geom);
		assertThat(feature.getState()).isEqualTo("ADEQUATE_120");
		assertThat(feature.getValueNumber()).isEqualByComparingTo("1.25");
	}
}
