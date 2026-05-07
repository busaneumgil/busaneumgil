package com.ssafy.e102.domain.route.entity;

import java.math.BigDecimal;

import org.locationtech.jts.geom.LineString;

import com.ssafy.e102.domain.route.type.AccessibilityState;
import com.ssafy.e102.domain.route.type.SegmentType;
import com.ssafy.e102.domain.route.type.SlopeState;
import com.ssafy.e102.domain.route.type.SurfaceState;
import com.ssafy.e102.domain.route.type.WidthState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "road_segments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadSegment {

	@Id
	@Column(name = "edgeId", nullable = false, updatable = false)
	private Long edgeId;

	@Column(name = "fromNodeId", nullable = false)
	private Long fromNodeId;

	@Column(name = "toNodeId", nullable = false)
	private Long toNodeId;

	@Column(name = "geom", nullable = false, columnDefinition = "geometry(LineString, 4326)")
	private LineString geom;

	@Column(name = "lengthMeter", nullable = false, precision = 10, scale = 2)
	private BigDecimal lengthMeter;

	@Enumerated(EnumType.STRING)
	@Column(name = "walkAccess", nullable = false, length = 30, columnDefinition = "varchar(30) default 'UNKNOWN'")
	private AccessibilityState walkAccess = AccessibilityState.UNKNOWN;

	@Column(name = "avgSlopePercent", precision = 6, scale = 2)
	private BigDecimal avgSlopePercent;

	@Column(name = "widthMeter", precision = 6, scale = 2)
	private BigDecimal widthMeter;

	@Enumerated(EnumType.STRING)
	@Column(name = "brailleBlockState", nullable = false, length = 30, columnDefinition = "varchar(30) default 'UNKNOWN'")
	private AccessibilityState brailleBlockState = AccessibilityState.UNKNOWN;

	@Enumerated(EnumType.STRING)
	@Column(name = "audioSignalState", nullable = false, length = 30, columnDefinition = "varchar(30) default 'UNKNOWN'")
	private AccessibilityState audioSignalState = AccessibilityState.UNKNOWN;

	@Enumerated(EnumType.STRING)
	@Column(name = "slopeState", nullable = false, length = 30, columnDefinition = "varchar(30) default 'UNKNOWN'")
	private SlopeState slopeState = SlopeState.UNKNOWN;

	@Enumerated(EnumType.STRING)
	@Column(name = "widthState", nullable = false, length = 30, columnDefinition = "varchar(30) default 'UNKNOWN'")
	private WidthState widthState = WidthState.UNKNOWN;

	@Enumerated(EnumType.STRING)
	@Column(name = "surfaceState", nullable = false, length = 30, columnDefinition = "varchar(30) default 'UNKNOWN'")
	private SurfaceState surfaceState = SurfaceState.UNKNOWN;

	@Enumerated(EnumType.STRING)
	@Column(name = "stairsState", nullable = false, length = 30, columnDefinition = "varchar(30) default 'UNKNOWN'")
	private AccessibilityState stairsState = AccessibilityState.UNKNOWN;

	@Enumerated(EnumType.STRING)
	@Column(name = "signalState", nullable = false, length = 30, columnDefinition = "varchar(30) default 'UNKNOWN'")
	private AccessibilityState signalState = AccessibilityState.UNKNOWN;

	@Enumerated(EnumType.STRING)
	@Column(name = "segmentType", nullable = false, length = 30, columnDefinition = "varchar(30) default 'SIDE_LINE'")
	private SegmentType segmentType = SegmentType.SIDE_LINE;

	public static RoadSegment create(
		Long edgeId,
		Long fromNodeId,
		Long toNodeId,
		LineString geom,
		BigDecimal lengthMeter) {
		RoadSegment roadSegment = new RoadSegment();
		roadSegment.edgeId = edgeId;
		roadSegment.fromNodeId = fromNodeId;
		roadSegment.toNodeId = toNodeId;
		roadSegment.geom = geom;
		roadSegment.lengthMeter = lengthMeter;
		return roadSegment;
	}
}
