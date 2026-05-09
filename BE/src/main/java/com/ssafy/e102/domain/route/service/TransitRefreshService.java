package com.ssafy.e102.domain.route.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.e102.domain.route.dto.request.TransitRefreshRequest;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.TransitArrivalStatus;
import com.ssafy.e102.domain.route.dto.response.TransitRefreshResponse;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.route.type.TransportMode;

@Service
@Transactional(readOnly = true)
public class TransitRefreshService {

	private final RouteSessionRepository routeSessionRepository;
	private final ObjectMapper objectMapper;

	public TransitRefreshService(RouteSessionRepository routeSessionRepository, ObjectMapper objectMapper) {
		this.routeSessionRepository = routeSessionRepository;
		this.objectMapper = objectMapper;
	}

	public TransitRefreshResponse refresh(UUID userId, String routeId, TransitRefreshRequest request) {
		RouteSession routeSession = getOwnedRouteSession(userId, routeId);
		RefreshTarget target = refreshTarget(routeSession, request.legSequence());
		if (target.leg().type() == TransportMode.WALK) {
			throw new RouteException(RouteErrorCode.NOT_TRANSIT_LEG);
		}
		return new TransitRefreshResponse(target.leg().type(), TransitArrivalStatus.ARRIVAL_UNKNOWN, List.of());
	}

	private RouteSession getOwnedRouteSession(UUID userId, String routeId) {
		return routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(userId, routeId)
			.orElseGet(() -> {
				if (routeSessionRepository.findFirstByRouteIdOrderByUpdatedAtDesc(routeId).isPresent()) {
					throw new RouteException(RouteErrorCode.ROUTE_ACCESS_DENIED);
				}
				throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
			});
	}

	private RefreshTarget refreshTarget(RouteSession routeSession, int legSequence) {
		RouteSummaryResponse route = restoreRouteSnapshot(routeSession);
		RouteLegResponse leg = route.legs()
			.stream()
			.filter(candidate -> candidate.sequence() == legSequence)
			.findFirst()
			.orElseThrow(() -> new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND));
		return new RefreshTarget(routeSession.getRouteSnapshotJson(), route, leg);
	}

	private RouteSummaryResponse restoreRouteSnapshot(RouteSession routeSession) {
		if (routeSession.getRouteSnapshotJson() == null || routeSession.getRouteSnapshotJson().isNull()) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
		}
		try {
			RouteSummaryResponse route = objectMapper.treeToValue(
				routePayloadSnapshot(routeSession.getRouteSnapshotJson()),
				RouteSummaryResponse.class);
			if (route == null || route.legs() == null || route.legs().isEmpty()) {
				throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
			}
			return route;
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND, "선택한 경로 정보를 복구할 수 없습니다.", exception);
		}
	}

	private JsonNode routePayloadSnapshot(JsonNode snapshot) {
		if (snapshot.has("backendMetadata") && snapshot instanceof ObjectNode objectNode) {
			ObjectNode routePayload = objectNode.deepCopy();
			routePayload.remove("backendMetadata");
			return routePayload;
		}
		return snapshot;
	}

	private record RefreshTarget(
		JsonNode snapshot,
		RouteSummaryResponse route,
		RouteLegResponse leg) {
	}
}
