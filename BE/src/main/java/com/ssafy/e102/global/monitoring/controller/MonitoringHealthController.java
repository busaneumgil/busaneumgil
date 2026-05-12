package com.ssafy.e102.global.monitoring.controller;

import java.util.Set;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class MonitoringHealthController {

	private static final Set<String> SUPPORTED_COMPONENTS = Set.of("db", "redis");

	private final HealthEndpoint healthEndpoint;

	public MonitoringHealthController(HealthEndpoint healthEndpoint) {
		this.healthEndpoint = healthEndpoint;
	}

	@GetMapping
	public ResponseEntity<HealthStatusResponse> overallHealth() {
		return toResponse("overall", healthEndpoint.health());
	}

	@GetMapping("/{component}")
	public ResponseEntity<HealthStatusResponse> componentHealth(@PathVariable String component) {
		if (!SUPPORTED_COMPONENTS.contains(component)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new HealthStatusResponse(component, "NOT_FOUND"));
		}
		return toResponse(component, healthEndpoint.healthForPath(component));
	}

	private ResponseEntity<HealthStatusResponse> toResponse(String component, HealthComponent healthComponent) {
		String status = healthComponent.getStatus().getCode();
		HttpStatus httpStatus = "UP".equalsIgnoreCase(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
		return ResponseEntity.status(httpStatus)
			.body(new HealthStatusResponse(component, status));
	}

	private record HealthStatusResponse(String component, String status) {
	}
}
