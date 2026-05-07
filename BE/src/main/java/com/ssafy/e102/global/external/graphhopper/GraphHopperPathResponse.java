package com.ssafy.e102.global.external.graphhopper;

import java.math.BigDecimal;
import java.util.List;

/**
 * GraphHopper paths[] 원소 중 도보 route payload에 필요한 거리, 시간, geometry를 받는다.
 *
 * <p>points.coordinates는 GeoJSON 순서인 lng/lat 배열이므로 {@link GraphHopperCoordinate}로 변환한다.
 */
public record GraphHopperPathResponse(
	BigDecimal distance,
	long time,
	GraphHopperPointsResponse points) {

	List<GraphHopperCoordinate> coordinates() {
		if (points == null || points.coordinates() == null) {
			return List.of();
		}
		// GraphHopper 좌표 배열은 [lng, lat] 순서라 내부 좌표 record로 명시적으로 옮긴다.
		return points.coordinates()
			.stream()
			.map(GraphHopperCoordinate::from)
			.toList();
	}
}
