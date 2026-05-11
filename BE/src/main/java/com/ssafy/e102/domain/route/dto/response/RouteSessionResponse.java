package com.ssafy.e102.domain.route.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.e102.domain.route.entity.RouteSession;

public record RouteSessionResponse(
	UUID sessionId,
	BigDecimal remainingDistanceMeter,
	Integer remainingDurationSecond) {

	public RouteSessionResponse(UUID sessionId) {
		this(sessionId, null, null);
	}

	public static RouteSessionResponse from(RouteSession routeSession) {
		return from(routeSession, routeSession.getRouteSnapshotJson());
	}

	public static RouteSessionResponse from(RouteSession routeSession, JsonNode routeSnapshotJson) {
		return new RouteSessionResponse(
			routeSession.getSessionId(),
			decimalOrNull(routeSnapshotJson, "distanceMeter"),
			integerOrNull(routeSnapshotJson, "durationSecond"));
	}

	private static BigDecimal decimalOrNull(JsonNode jsonNode, String fieldName) {
		if (jsonNode == null || !jsonNode.hasNonNull(fieldName) || !jsonNode.get(fieldName).isNumber()) {
			return null;
		}
		return jsonNode.get(fieldName).decimalValue();
	}

	private static Integer integerOrNull(JsonNode jsonNode, String fieldName) {
		if (jsonNode == null || !jsonNode.hasNonNull(fieldName) || !jsonNode.get(fieldName).canConvertToInt()) {
			return null;
		}
		return jsonNode.get(fieldName).intValue();
	}
}
