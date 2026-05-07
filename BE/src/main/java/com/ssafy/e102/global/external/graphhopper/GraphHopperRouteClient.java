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

@Component
public class GraphHopperRouteClient {

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
		return new GraphHopperRoutePath(path.distance(), path.time(), coordinates);
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
