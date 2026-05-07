package com.ssafy.e102.domain.route.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import com.ssafy.e102.domain.route.type.RouteStepAlertType;

public class RouteTurnInstructionResolver {

	private static final double DEFAULT_MIN_TURN_DEGREES = 35.0;

	public RouteStepAlertType resolve(Coordinate previous, Coordinate pivot, Coordinate next) {
		return resolve(previous, pivot, next, DEFAULT_MIN_TURN_DEGREES);
	}

	public RouteStepAlertType resolve(Coordinate previous, Coordinate pivot, Coordinate next, double minTurnDegrees) {
		if (previous == null || pivot == null || next == null) {
			return RouteStepAlertType.NONE;
		}

		double incomingX = pivot.x - previous.x;
		double incomingY = pivot.y - previous.y;
		double outgoingX = next.x - pivot.x;
		double outgoingY = next.y - pivot.y;
		if (isZeroVector(incomingX, incomingY) || isZeroVector(outgoingX, outgoingY)) {
			return RouteStepAlertType.NONE;
		}

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
