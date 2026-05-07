package com.ssafy.e102.global.external.odsay;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

@Component
public class OdsayClient {

	private static final int MAX_INTERNAL_CANDIDATES = 10;
	private static final int SEARCH_TYPE_ALL = 0;

	private final RestTemplate restTemplate;
	private final OdsayProperties properties;

	public OdsayClient(RestTemplateBuilder builder, OdsayProperties properties) {
		this.restTemplate = builder
			.connectTimeout(properties.connectTimeout())
			.readTimeout(properties.readTimeout())
			.build();
		this.properties = properties;
	}

	public OdsayTransitSearchResult searchPubTransPath(GeoPointRequest startPoint, GeoPointRequest endPoint) {
		if (!StringUtils.hasText(properties.apiKey())) {
			throw new RouteException(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED, "ODsay API key가 설정되지 않았습니다.");
		}
		try {
			JsonNode body = restTemplate.exchange(
				RequestEntity
					.method(HttpMethod.GET, UriComponentsBuilder
						.fromUriString(properties.baseUrl())
						.path("/searchPubTransPathT")
						.queryParam("SX", startPoint.lng())
						.queryParam("SY", startPoint.lat())
						.queryParam("EX", endPoint.lng())
						.queryParam("EY", endPoint.lat())
						.queryParam("SearchType", SEARCH_TYPE_ALL)
						.queryParam("apiKey", properties.apiKey())
						.build()
						.toUri())
					.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
					.build(),
				JsonNode.class)
				.getBody();
			return parseSearchResult(body);
		} catch (HttpStatusCodeException exception) {
			throw externalFailure(exception);
		} catch (ResourceAccessException exception) {
			throw new RouteException(timeoutOrFailure(exception), timeoutOrFailure(exception).getMessage(), exception);
		} catch (RestClientException exception) {
			throw new RouteException(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED,
				RouteErrorCode.EXTERNAL_ROUTE_API_FAILED.getMessage(), exception);
		}
	}

	private OdsayTransitSearchResult parseSearchResult(JsonNode body) {
		JsonNode pathNodes = body == null ? null : body.path("result").path("path");
		if (pathNodes == null || !pathNodes.isArray() || pathNodes.isEmpty()) {
			throw new RouteException(RouteErrorCode.ROUTE_NOT_FOUND);
		}
		List<OdsayTransitPath> paths = new ArrayList<>();
		for (JsonNode pathNode : pathNodes) {
			if (paths.size() >= MAX_INTERNAL_CANDIDATES) {
				break;
			}
			paths.add(parsePath(pathNode));
		}
		return new OdsayTransitSearchResult(List.copyOf(paths));
	}

	private OdsayTransitPath parsePath(JsonNode pathNode) {
		JsonNode info = pathNode.path("info");
		List<OdsayTransitLeg> legs = new ArrayList<>();
		for (JsonNode legNode : pathNode.path("subPath")) {
			TransportMode type = toTransportMode(legNode.path("trafficType").asInt());
			legs.add(parseLeg(type, legNode));
		}
		return new OdsayTransitPath(
			decimal(info, "totalDistance"),
			info.path("totalTime").asInt(),
			info.path("totalWalk").asInt(),
			info.path("busTransitCount").asInt(),
			info.path("subwayTransitCount").asInt(),
			text(info, "mapObj"),
			List.copyOf(legs),
			snapshot(
				"mapObj", text(info, "mapObj"),
				"pathType", pathNode.path("pathType").asInt(),
				"info", info));
	}

	private OdsayTransitLeg parseLeg(TransportMode type, JsonNode legNode) {
		return new OdsayTransitLeg(
			type,
			decimal(legNode, "distance"),
			legNode.path("sectionTime").asInt(),
			text(legNode, "startName"),
			decimal(legNode, "startY"),
			decimal(legNode, "startX"),
			text(legNode, "startID"),
			text(legNode, "startLocalStationID"),
			text(legNode, "startArsID"),
			text(legNode, "endName"),
			decimal(legNode, "endY"),
			decimal(legNode, "endX"),
			text(legNode, "endID"),
			text(legNode, "endLocalStationID"),
			text(legNode, "endArsID"),
			parseLanes(legNode.path("lane")),
			parsePassStops(legNode.path("passStopList").path("stations")),
			snapshot(
				"trafficType", legNode.path("trafficType").asInt(),
				"startID", text(legNode, "startID"),
				"endID", text(legNode, "endID"),
				"startLocalStationID", text(legNode, "startLocalStationID"),
				"endLocalStationID", text(legNode, "endLocalStationID"),
				"startArsID", text(legNode, "startArsID"),
				"endArsID", text(legNode, "endArsID")));
	}

	private List<OdsayTransitLane> parseLanes(JsonNode laneNodes) {
		if (!laneNodes.isArray()) {
			return List.of();
		}
		List<OdsayTransitLane> lanes = new ArrayList<>();
		for (JsonNode laneNode : laneNodes) {
			lanes.add(new OdsayTransitLane(
				text(laneNode, "busNo"),
				text(laneNode, "busLocalBlID"),
				text(laneNode, "subwayCode"),
				text(laneNode, "name")));
		}
		return List.copyOf(lanes);
	}

	private List<OdsayPassStop> parsePassStops(JsonNode stationNodes) {
		if (!stationNodes.isArray()) {
			return List.of();
		}
		List<OdsayPassStop> stops = new ArrayList<>();
		for (JsonNode stationNode : stationNodes) {
			stops.add(new OdsayPassStop(
				text(stationNode, "stationID"),
				text(stationNode, "stationName"),
				text(stationNode, "localStationID"),
				text(stationNode, "arsID"),
				decimal(stationNode, "y"),
				decimal(stationNode, "x")));
		}
		return List.copyOf(stops);
	}

	private TransportMode toTransportMode(int trafficType) {
		return switch (trafficType) {
			case 1 -> TransportMode.SUBWAY;
			case 2 -> TransportMode.BUS;
			case 3 -> TransportMode.WALK;
			default ->
				throw new RouteException(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED, "알 수 없는 ODsay trafficType입니다.");
		};
	}

	private String text(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		if (value.isMissingNode() || value.isNull()) {
			return null;
		}
		String text = value.asText();
		return StringUtils.hasText(text) ? text : null;
	}

	private BigDecimal decimal(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		if (value.isMissingNode() || value.isNull() || !StringUtils.hasText(value.asText())) {
			return null;
		}
		return new BigDecimal(value.asText());
	}

	private RouteException externalFailure(HttpStatusCodeException exception) {
		return new RouteException(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED,
			RouteErrorCode.EXTERNAL_ROUTE_API_FAILED.getMessage(), exception);
	}

	private RouteErrorCode timeoutOrFailure(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SocketTimeoutException) {
				return RouteErrorCode.EXTERNAL_ROUTE_API_TIMEOUT;
			}
			current = current.getCause();
		}
		return RouteErrorCode.EXTERNAL_ROUTE_API_FAILED;
	}

	private Map<String, Object> snapshot(Object... values) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		for (int index = 0; index < values.length; index += 2) {
			snapshot.put((String)values[index], values[index + 1]);
		}
		return snapshot;
	}
}
