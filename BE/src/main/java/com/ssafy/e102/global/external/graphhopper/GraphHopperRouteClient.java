package com.ssafy.e102.global.external.graphhopper;

import java.net.URI;
import java.net.SocketTimeoutException;
import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;

/**
 * Backend route service에서 GraphHopper runtime의 `/route` API로 나가는 단일 통로다.
 *
 * <p>호출 흐름은 route service -> GraphHopperRouteClient -> GraphHopper `/route` -> 정제된
 * {@link GraphHopperRoutePath} 반환 순서다. HTTP 실패와 timeout은 route 도메인 에러 코드로 변환한다.
 */
@Component
public class GraphHopperRouteClient {

	private static final List<String> WALK_PATH_DETAILS = List.of(
		"edge_id",
		"segment_type",
		"signal_state",
		"audio_signal_state",
		"slope_state",
		"avg_slope_percent",
		"width_state",
		"surface_state",
		"stairs_state");

	private final RestTemplate restTemplate;
	private final GraphHopperProperties properties;

	@Autowired
	public GraphHopperRouteClient(RestTemplateBuilder builder, GraphHopperProperties properties) {
		this(builder
			.connectTimeout(properties.connectTimeout())
			.readTimeout(properties.readTimeout())
			.build(), properties);
	}

	GraphHopperRouteClient(RestTemplate restTemplate, GraphHopperProperties properties) {
		this.properties = properties;
		this.restTemplate = restTemplate;
	}

	public GraphHopperRoutePath route(GraphHopperRouteRequest request) {
		try {
			// GraphHopper는 GET query 기반 API라 profile과 두 point를 URI에 직접 싣는다.
			GraphHopperRouteResponse response = restTemplate.exchange(
				RequestEntity
					.method(HttpMethod.GET, routeUri(request))
					.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
					.build(),
				GraphHopperRouteResponse.class)
				.getBody();
			return extractFirstPath(response);
		} catch (HttpStatusCodeException exception) {
			throw new RouteException(
				RouteErrorCode.EXTERNAL_ROUTE_API_FAILED,
				RouteErrorCode.EXTERNAL_ROUTE_API_FAILED.getMessage(),
				exception);
		} catch (ResourceAccessException exception) {
			RouteErrorCode errorCode = hasTimeoutCause(exception)
				? RouteErrorCode.EXTERNAL_ROUTE_API_TIMEOUT
				: RouteErrorCode.EXTERNAL_ROUTE_API_FAILED;
			throw new RouteException(errorCode, errorCode.getMessage(), exception);
		} catch (RestClientException exception) {
			throw new RouteException(
				RouteErrorCode.EXTERNAL_ROUTE_API_FAILED,
				RouteErrorCode.EXTERNAL_ROUTE_API_FAILED.getMessage(),
				exception);
		}
	}

	private URI routeUri(GraphHopperRouteRequest request) {
		return UriComponentsBuilder
			.fromUriString(properties.baseUrl())
			.path("/route")
			.queryParam("profile", request.profile().getProfileName())
			.queryParam("point", point(request.startPoint()))
			.queryParam("point", point(request.endPoint()))
			.queryParam("points_encoded", "false")
			.queryParam("locale", "ko-KR")
			.queryParam("details", WALK_PATH_DETAILS.toArray())
			.build()
			.toUri();
	}

	private String point(com.ssafy.e102.global.geo.dto.GeoPointRequest point) {
		return point.lat() + "," + point.lng();
	}

	private GraphHopperRoutePath extractFirstPath(GraphHopperRouteResponse response) {
		if (response == null || response.paths() == null || response.paths().isEmpty()) {
			throw new RouteException(RouteErrorCode.ROUTE_NOT_FOUND);
		}
		GraphHopperPathResponse path = response.paths().get(0);
		List<GraphHopperCoordinate> coordinates = path.coordinates();
		if (coordinates.isEmpty()) {
			throw new RouteException(RouteErrorCode.ROUTE_NOT_FOUND);
		}
		// 이후 step/payload service는 GraphHopper 원본 JSON이 아니라 이 정제된 path만 사용한다.
		return new GraphHopperRoutePath(path.distance(), path.time(), coordinates, path.pathDetails());
	}

	private boolean hasTimeoutCause(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SocketTimeoutException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
