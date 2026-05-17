package com.ssafy.e102.global.external.graphhopper;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
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

import com.ssafy.e102.domain.route.type.AccessibilityState;

@Component
public class GraphHopperAdminClient {

	private static final Logger log = LoggerFactory.getLogger(GraphHopperAdminClient.class);
	private static final int MAX_SINGLE_ENDPOINT_ATTEMPTS = 2;

	private final RestTemplate restTemplate;
	private final GraphHopperEndpointProvider endpointProvider;

	@Autowired
	public GraphHopperAdminClient(
		RestTemplateBuilder builder,
		GraphHopperProperties properties,
		GraphHopperEndpointProvider endpointProvider) {
		this(
			builder.connectTimeout(properties.connectTimeout()).readTimeout(properties.readTimeout()).build(),
			endpointProvider);
	}

	GraphHopperAdminClient(RestTemplate restTemplate, GraphHopperProperties properties) {
		this(restTemplate, () -> GraphHopperEndpointSelection.fallback(properties.baseUrl()));
	}

	GraphHopperAdminClient(RestTemplate restTemplate, GraphHopperEndpointProvider endpointProvider) {
		this.restTemplate = restTemplate;
		this.endpointProvider = endpointProvider;
	}

	public GraphHopperPatchResult patchWalkAccess(long edgeId, AccessibilityState walkAccess) {
		GraphHopperEndpointSelection endpointSelection = endpointProvider.selectEndpoint();
		List<EndpointTarget> endpointTargets = resolveTargets(endpointSelection);
		List<String> patchedSlots = new ArrayList<>();
		List<String> failureMessages = new ArrayList<>();

		for (EndpointTarget endpointTarget : endpointTargets) {
			EndpointPatchResult patchResult = patchEndpointWithRetry(endpointTarget, edgeId, walkAccess);
			if (patchResult.success()) {
				patchedSlots.add(endpointTarget.slot());
				continue;
			}
			failureMessages.add(patchResult.message());
		}

		if (failureMessages.isEmpty()) {
			return new GraphHopperPatchResult(
				GraphHopperPatchStatus.APPLIED,
				"Patched GraphHopper slot(s): " + String.join(", ", patchedSlots));
		}
		if (!patchedSlots.isEmpty()) {
			return new GraphHopperPatchResult(
				GraphHopperPatchStatus.APPLIED_WITH_WARNING,
				"Patched GraphHopper slot(s): "
					+ String.join(", ", patchedSlots)
					+ " | failed slot(s): "
					+ String.join(" | ", failureMessages));
		}
		return new GraphHopperPatchResult(
			GraphHopperPatchStatus.FAILED,
			"GraphHopper patch failed: " + String.join(" | ", failureMessages));
	}

	private EndpointPatchResult patchEndpointWithRetry(
		EndpointTarget endpointTarget,
		long edgeId,
		AccessibilityState walkAccess) {
		RuntimeException lastException = null;
		for (int attempt = 1; attempt <= MAX_SINGLE_ENDPOINT_ATTEMPTS; attempt++) {
			try {
				patchWalkAccessOnce(endpointTarget.baseUrl(), edgeId, walkAccess);
				return EndpointPatchResult.succeeded();
			} catch (HttpStatusCodeException | ResourceAccessException exception) {
				lastException = exception;
				log.warn(
					"graphhopper hot patch failed slot={} edgeId={} walkAccess={} attempt={} message={}",
					endpointTarget.slot(),
					edgeId,
					walkAccess,
					attempt,
					exception.getMessage(),
					exception);
			} catch (RestClientException exception) {
				lastException = exception;
				log.warn(
					"graphhopper hot patch failed slot={} edgeId={} walkAccess={} attempt={} message={}",
					endpointTarget.slot(),
					edgeId,
					walkAccess,
					attempt,
					exception.getMessage(),
					exception);
			}
		}
		return EndpointPatchResult.failed(
			"slot=" + endpointTarget.slot() + " edgeId=" + edgeId + " message=" + describeFailure(lastException));
	}

	private void patchWalkAccessOnce(String baseUrl, long edgeId, AccessibilityState walkAccess) {
		restTemplate.exchange(
			RequestEntity
				.method(HttpMethod.PATCH, patchWalkAccessUri(baseUrl, edgeId))
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("walkAccess", walkAccess.name())),
			Void.class);
	}

	private java.net.URI patchWalkAccessUri(String baseUrl, long edgeId) {
		return UriComponentsBuilder
			.fromUriString(baseUrl)
			.path("/ieum/admin/edges/{edgeId}/walk-access")
			.buildAndExpand(Map.of("edgeId", edgeId))
			.toUri();
	}

	private List<EndpointTarget> resolveTargets(GraphHopperEndpointSelection endpointSelection) {
		Map<String, EndpointTarget> targetsByBaseUrl = new LinkedHashMap<>();
		targetsByBaseUrl.put(
			endpointSelection.activeBaseUrl(),
			new EndpointTarget(endpointSelection.activeBaseUrl(), endpointSelection.activeSlot()));
		if (endpointSelection.hasPrevious()) {
			targetsByBaseUrl.putIfAbsent(
				endpointSelection.previousBaseUrl(),
				new EndpointTarget(endpointSelection.previousBaseUrl(), endpointSelection.previousSlot()));
		}
		return List.copyOf(targetsByBaseUrl.values());
	}

	private String describeFailure(RuntimeException exception) {
		if (exception == null) {
			return "unknown";
		}
		if (exception instanceof HttpStatusCodeException httpStatusCodeException) {
			return httpStatusCodeException.getStatusCode() + " " + httpStatusCodeException.getResponseBodyAsString();
		}
		if (exception instanceof ResourceAccessException resourceAccessException) {
			return hasTimeoutCause(resourceAccessException) ? "timeout" : resourceAccessException.getMessage();
		}
		return exception.getMessage();
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

	public record GraphHopperPatchResult(GraphHopperPatchStatus status, String message) {
	}

	public enum GraphHopperPatchStatus {
		SKIPPED,
		APPLIED,
		APPLIED_WITH_WARNING,
		FAILED
	}

	private record EndpointTarget(String baseUrl, String slot) {
	}

	private record EndpointPatchResult(boolean success, String message) {
		private static EndpointPatchResult succeeded() {
			return new EndpointPatchResult(true, null);
		}

		private static EndpointPatchResult failed(String message) {
			return new EndpointPatchResult(false, message);
		}
	}
}
