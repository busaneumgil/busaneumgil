package com.ssafy.e102.domain.route.entity;

import java.math.BigDecimal;

import org.locationtech.jts.geom.Geometry;

import com.ssafy.e102.domain.route.type.SegmentFeatureType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GraphHopper export 전에 road segment를 feature 위치 기준으로 분할하고 접근성 상태를 덮어쓰기 위한 원천 feature다.
 */
@Getter
@Entity
@Table(name = "segment_features")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SegmentFeature {

	@Id
	@Column(name = "feature_id", nullable = false, updatable = false)
	private Long featureId;

	@Column(name = "edge_id", nullable = false)
	private Long edgeId;

	@Enumerated(EnumType.STRING)
	@Column(name = "feature_type", nullable = false, length = 50)
	private SegmentFeatureType featureType;

	@Column(name = "geom", nullable = false, columnDefinition = "geometry(Geometry, 4326)")
	private Geometry geom;

	@Column(name = "state", length = 50)
	private String state;

	@Column(name = "value_number", precision = 10, scale = 2)
	private BigDecimal valueNumber;

	public static SegmentFeature create(
		Long featureId,
		Long edgeId,
		SegmentFeatureType featureType,
		Geometry geom,
		String state,
		BigDecimal valueNumber) {
		SegmentFeature segmentFeature = new SegmentFeature();
		segmentFeature.featureId = featureId;
		segmentFeature.edgeId = edgeId;
		segmentFeature.featureType = featureType;
		segmentFeature.geom = geom;
		segmentFeature.state = state;
		segmentFeature.valueNumber = valueNumber;
		return segmentFeature;
	}
}
