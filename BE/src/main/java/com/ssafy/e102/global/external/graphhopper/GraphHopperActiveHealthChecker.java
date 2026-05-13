package com.ssafy.e102.global.external.graphhopper;

import java.net.URI;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Redis active slot 기준으로 현재 backend가 사용할 GraphHopper runtime health를 확인한다.
 */
@Component
public class GraphHopperActiveHealthChecker {

	private static final String UP = "UP";
	private static final String DOWN = "DOWN";

	private final RestTemplate restTemplate;
	private final GraphHopperEndpointProvider endpointProvider;

	public GraphHopperActiveHealthChecker(
		RestTemplateBuilder builder,
		GraphHopperProperties properties,
		GraphHopperEndpointProvider endpointProvider) {
		this(builder
			.connectTimeout(properties.connectTimeout())
			.readTimeout(properties.readTimeout())
			.build(), endpointProvider);
	}

	GraphHopperActiveHealthChecker(RestTemplate restTemplate, GraphHopperEndpointProvider endpointProvider) {
		this.restTemplate = restTemplate;
		this.endpointProvider = endpointProvider;
	}

	public GraphHopperHealthStatus check() {
		GraphHopperEndpointSelection endpoint = endpointProvider.selectEndpoint();
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(healthcheckUri(endpoint.activeBaseUrl()),
				String.class);
			String status = response.getStatusCode().is2xxSuccessful() ? UP : DOWN;
			return GraphHopperHealthStatus.of(status, endpoint);
		} catch (RestClientException exception) {
			return GraphHopperHealthStatus.of(DOWN, endpoint);
		}
	}

	private URI healthcheckUri(String baseUrl) {
		String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return URI.create(normalized + "/healthcheck");
	}

	public record GraphHopperHealthStatus(
		String status,
		String activeSlot,
		String previousSlot) {

		private static GraphHopperHealthStatus of(String status, GraphHopperEndpointSelection endpoint) {
			return new GraphHopperHealthStatus(status, endpoint.activeSlot(), endpoint.previousSlot());
		}
	}
}
