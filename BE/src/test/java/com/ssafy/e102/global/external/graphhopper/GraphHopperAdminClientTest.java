package com.ssafy.e102.global.external.graphhopper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.ssafy.e102.domain.route.type.AccessibilityState;
import com.ssafy.e102.global.external.graphhopper.GraphHopperAdminClient.GraphHopperPatchStatus;

class GraphHopperAdminClientTest {

	private RestTemplate restTemplate;
	private MockRestServiceServer server;
	private GraphHopperAdminClient client;

	@BeforeEach
	void setUp() {
		restTemplate = new RestTemplate();
		server = MockRestServiceServer.createServer(restTemplate);
		client = new GraphHopperAdminClient(
			restTemplate,
			() -> new GraphHopperEndpointSelection(
				"http://graphhopper-green.test",
				"http://graphhopper-blue.test",
				"green",
				"blue"));
	}

	@Test
	@DisplayName("GraphHopper hot patch는 active와 previous slot에 모두 적용한다")
	void patchWalkAccessAppliesToActiveAndPreviousSlots() {
		server.expect(requestTo("http://graphhopper-green.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		server.expect(requestTo("http://graphhopper-blue.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		GraphHopperAdminClient.GraphHopperPatchResult result = client.patchWalkAccess(123L, AccessibilityState.NO);

		assertThat(result.status()).isEqualTo(GraphHopperPatchStatus.APPLIED);
		assertThat(result.message()).contains("green", "blue");
		server.verify();
	}

	@Test
	@DisplayName("GraphHopper hot patch는 일부 slot만 성공하면 경고 상태를 반환한다")
	void patchWalkAccessReturnsAppliedWithWarningWhenPartialSuccess() {
		server.expect(requestTo("http://graphhopper-green.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		server.expect(requestTo("http://graphhopper-blue.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withServerError());
		server.expect(requestTo("http://graphhopper-blue.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withServerError());

		GraphHopperAdminClient.GraphHopperPatchResult result = client.patchWalkAccess(123L, AccessibilityState.NO);

		assertThat(result.status()).isEqualTo(GraphHopperPatchStatus.APPLIED_WITH_WARNING);
		assertThat(result.message()).contains("Patched GraphHopper slot(s): green", "slot=blue");
		server.verify();
	}

	@Test
	@DisplayName("GraphHopper hot patch는 active 실패 후 previous만 성공해도 FAILED를 반환한다")
	void patchWalkAccessReturnsFailedWhenOnlyPreviousSucceeds() {
		server.expect(requestTo("http://graphhopper-green.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withServerError());
		server.expect(requestTo("http://graphhopper-green.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withServerError());
		server.expect(requestTo("http://graphhopper-blue.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		GraphHopperAdminClient.GraphHopperPatchResult result = client.patchWalkAccess(123L, AccessibilityState.NO);

		assertThat(result.status()).isEqualTo(GraphHopperPatchStatus.FAILED);
		assertThat(result.message()).contains("slot=green");
		server.verify();
	}

	@Test
	@DisplayName("GraphHopper hot patch 실패 시 동일 endpoint에 즉시 재시도 후 FAILED를 반환한다")
	void patchWalkAccessRetriesAndReturnsFailed() {
		server.expect(requestTo("http://graphhopper-green.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withServerError());
		server.expect(requestTo("http://graphhopper-green.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withServerError());
		server.expect(requestTo("http://graphhopper-blue.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withServerError());
		server.expect(requestTo("http://graphhopper-blue.test/ieum/admin/edges/123/walk-access"))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withServerError());

		GraphHopperAdminClient.GraphHopperPatchResult result = client.patchWalkAccess(123L, AccessibilityState.NO);

		assertThat(result.status()).isEqualTo(GraphHopperPatchStatus.FAILED);
		assertThat(result.message()).contains("slot=green", "slot=blue");
		server.verify();
	}
}
