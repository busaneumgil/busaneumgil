package com.ssafy.e102.domain.route.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.e102.domain.route.dto.request.TransitRefreshRequest;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.TransitArrivalResponse;
import com.ssafy.e102.domain.route.dto.response.TransitArrivalStatus;
import com.ssafy.e102.domain.route.dto.response.TransitRefreshResponse;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.global.external.bims.BusanBimsArrival;
import com.ssafy.e102.global.external.bims.BusanBimsClient;

@Service
@Transactional(readOnly = true)
public class TransitRefreshService {

	private final RouteSessionRepository routeSessionRepository;
	private final ObjectMapper objectMapper;
	private final BimsArrivalCacheService bimsArrivalCacheService;
	private final BusanBimsClient busanBimsClient;

	public TransitRefreshService(
		RouteSessionRepository routeSessionRepository,
		ObjectMapper objectMapper,
		BimsArrivalCacheService bimsArrivalCacheService,
		BusanBimsClient busanBimsClient) {
		this.routeSessionRepository = routeSessionRepository;
		this.objectMapper = objectMapper;
		this.bimsArrivalCacheService = bimsArrivalCacheService;
		this.busanBimsClient = busanBimsClient;
	}

	public TransitRefreshResponse refresh(UUID userId, String routeId, TransitRefreshRequest request) {
		RouteSession routeSession = getOwnedRouteSession(userId, routeId);
		RefreshTarget target = refreshTarget(routeSession, request.legSequence());
		if (target.leg().type() == TransportMode.WALK) {
			throw new RouteException(RouteErrorCode.NOT_TRANSIT_LEG);
		}
		if (target.leg().type() == TransportMode.BUS) {
			return refreshBus(target);
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
		return new RefreshTarget(routeSession.getRouteSnapshotJson(), route, leg,
			metadataLeg(route, leg, routeSession));
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

	private JsonNode metadataLeg(RouteSummaryResponse route, RouteLegResponse leg, RouteSession routeSession) {
		JsonNode snapshot = routeSession.getRouteSnapshotJson();
		JsonNode metadata = snapshot == null ? null : snapshot.get("backendMetadata");
		JsonNode legs = metadata == null ? null : metadata.get("legs");
		if (legs == null || !legs.isArray()) {
			return null;
		}
		for (JsonNode candidate : legs) {
			if (candidate.path("legSequence").asInt(-1) == leg.sequence()) {
				return candidate;
			}
		}
		int targetOrdinal = transitOrdinal(route, leg);
		int ordinal = 0;
		for (JsonNode candidate : legs) {
			if (sameTransportMode(candidate, leg.type())) {
				ordinal++;
				if (ordinal == targetOrdinal) {
					return candidate;
				}
			}
		}
		return null;
	}

	private int transitOrdinal(RouteSummaryResponse route, RouteLegResponse targetLeg) {
		int ordinal = 0;
		for (RouteLegResponse leg : route.legs()) {
			if (leg.type() == targetLeg.type()) {
				ordinal++;
			}
			if (leg.sequence() == targetLeg.sequence()) {
				return ordinal;
			}
		}
		return ordinal;
	}

	private boolean sameTransportMode(JsonNode node, TransportMode transportMode) {
		return transportMode.name().equals(node.path("type").asText());
	}

	private TransitRefreshResponse refreshBus(RefreshTarget target) {
		String stopId = boardingStopId(target.metadataLeg());
		List<BusLane> lanes = busLanes(target);
		if (!StringUtils.hasText(stopId) || lanes.isEmpty()) {
			return new TransitRefreshResponse(TransportMode.BUS, TransitArrivalStatus.ARRIVAL_UNKNOWN, List.of());
		}
		List<TransitArrivalResponse> transits = lanes.stream()
			.map(lane -> busArrival(stopId, lane))
			.filter(arrival -> arrival.remainingMinute() != null)
			.map(arrival -> new TransitArrivalResponse(
				arrival.routeNo(),
				arrival.remainingMinute(),
				arrival.isLowFloor()))
			.toList();
		if (transits.isEmpty()) {
			return new TransitRefreshResponse(TransportMode.BUS, TransitArrivalStatus.NO_CURRENT_ARRIVAL, List.of());
		}
		return new TransitRefreshResponse(TransportMode.BUS, TransitArrivalStatus.REALTIME_AVAILABLE, transits);
	}

	private String boardingStopId(JsonNode metadataLeg) {
		JsonNode passStops = metadataLeg == null ? null : metadataLeg.get("passStops");
		if (passStops != null && passStops.isArray() && !passStops.isEmpty()) {
			String stopId = text(passStops.get(0), "localStationID");
			if (StringUtils.hasText(stopId)) {
				return stopId;
			}
		}
		return text(metadataLeg, "startLocalStationId");
	}

	private List<BusLane> busLanes(RefreshTarget target) {
		JsonNode lanes = target.metadataLeg() == null ? null : target.metadataLeg().get("lanes");
		if (lanes != null && lanes.isArray() && !lanes.isEmpty()) {
			return StreamSupport.stream(lanes.spliterator(), false)
				.map(lane -> new BusLane(text(lane, "busNo"), text(lane, "busLocalBlID")))
				.filter(lane -> StringUtils.hasText(lane.routeNo()))
				.toList();
		}
		if (target.leg().laneOptions() != null && !target.leg().laneOptions().isEmpty()) {
			return target.leg().laneOptions()
				.stream()
				.map(lane -> new BusLane(lane.routeNo(), null))
				.toList();
		}
		return List.of(new BusLane(target.leg().routeNo(), null)).stream()
			.filter(lane -> StringUtils.hasText(lane.routeNo()))
			.toList();
	}

	private BusanBimsArrival busArrival(String stopId, BusLane lane) {
		if (!StringUtils.hasText(lane.lineId())) {
			return busanBimsClient.findArrival(stopId, null, lane.routeNo());
		}
		return bimsArrivalCacheService.find(stopId, lane.lineId())
			.orElseGet(() -> {
				BusanBimsArrival arrival = busanBimsClient.findArrival(stopId, lane.lineId(), lane.routeNo());
				if (StringUtils.hasText(arrival.stopId()) && StringUtils.hasText(arrival.lineId())) {
					bimsArrivalCacheService.save(arrival);
				}
				return arrival;
			});
	}

	private String text(JsonNode node, String fieldName) {
		if (node == null || !node.hasNonNull(fieldName)) {
			return null;
		}
		String value = node.get(fieldName).asText();
		return StringUtils.hasText(value) ? value : null;
	}

	private record RefreshTarget(
		JsonNode snapshot,
		RouteSummaryResponse route,
		RouteLegResponse leg,
		JsonNode metadataLeg) {
	}

	private record BusLane(
		String routeNo,
		String lineId) {
		BusLane {
			routeNo = Objects.requireNonNullElse(routeNo, "");
		}
	}
}
