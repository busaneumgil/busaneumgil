package com.ssafy.e102.domain.route.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;

import com.ssafy.e102.domain.route.dto.response.RouteStepAlertType;

/**
 * route geometry만으로 회전 안내를 계산하는 서비스다.
 *
 * <p>이 서비스는 {@code segment_features}에 의존하지 않는다. Feature row는 road segment 분할/export에만
 * 사용하고, {@code TURN_LEFT}/{@code TURN_RIGHT} 안내는 연속 route 좌표의 signed heading 변화에서 계산한다.
 */
@Service
public class RouteTurnInstructionService {

	private static final double DEFAULT_MIN_TURN_DEGREES = 35.0;

	public RouteStepAlertType resolve(Coordinate previous, Coordinate pivot, Coordinate next) {
		return resolve(previous, pivot, next, DEFAULT_MIN_TURN_DEGREES);
	}

	public RouteStepAlertType resolve(Coordinate previous, Coordinate pivot, Coordinate next, double minTurnDegrees) {
		if (previous == null || pivot == null || next == null) {
			return RouteStepAlertType.NONE;
		}

		// incoming/outgoing vector는 pivot 전후의 route 진행 방향을 나타낸다.
		double incomingX = pivot.x - previous.x;
		double incomingY = pivot.y - previous.y;
		double outgoingX = next.x - pivot.x;
		double outgoingY = next.y - pivot.y;
		if (isZeroVector(incomingX, incomingY) || isZeroVector(outgoingX, outgoingY)) {
			return RouteStepAlertType.NONE;
		}

		// atan2(cross, dot)은 signed angle을 돌려준다. JTS x/y 좌표계에서는 양수가 좌회전이다.
		double cross = incomingX * outgoingY - incomingY * outgoingX;
		double dot = incomingX * outgoingX + incomingY * outgoingY;
		double signedDegrees = Math.toDegrees(Math.atan2(cross, dot));
		if (Math.abs(signedDegrees) < minTurnDegrees) {
			return RouteStepAlertType.NONE;
		}
		return signedDegrees > 0.0 ? RouteStepAlertType.TURN_LEFT : RouteStepAlertType.TURN_RIGHT;
	}

	public RouteStepAlertType resolve(LineString routeGeometry, int pivotCoordinateIndex) {
		if (routeGeometry == null || pivotCoordinateIndex <= 0
			|| pivotCoordinateIndex >= routeGeometry.getNumPoints() - 1) {
			return RouteStepAlertType.NONE;
		}
		return resolve(
			routeGeometry.getCoordinateN(pivotCoordinateIndex - 1),
			routeGeometry.getCoordinateN(pivotCoordinateIndex),
			routeGeometry.getCoordinateN(pivotCoordinateIndex + 1));
	}

	private boolean isZeroVector(double x, double y) {
		return x == 0.0 && y == 0.0;
	}
}
