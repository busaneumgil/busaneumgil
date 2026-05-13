package com.ssafy.e102.global.external.graphhopper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GraphHopperActiveHealthCheckerTest {

	@Test
	@DisplayName("active slot GraphHopper healthcheck가 성공하면 UP을 반환한다")
	void activeSlotHealthcheckUp() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		GraphHopperActiveHealthChecker checker = new GraphHopperActiveHealthChecker(
			restTemplate,
			() -> new GraphHopperEndpointSelection(
				"http://graphhopper-green.test",
				"http://graphhopper-blue.test",
				"green",
				"blue"));
		server.expect(requestTo("http://graphhopper-green.test/healthcheck"))
			.andRespond(withSuccess("OK", MediaType.TEXT_PLAIN));

		GraphHopperActiveHealthChecker.GraphHopperHealthStatus status = checker.check();

		assertThat(status.status()).isEqualTo("UP");
		assertThat(status.activeSlot()).isEqualTo("green");
		assertThat(status.previousSlot()).isEqualTo("blue");
		server.verify();
	}

	@Test
	@DisplayName("active slot GraphHopper healthcheck가 실패하면 DOWN을 반환한다")
	void activeSlotHealthcheckDown() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		GraphHopperActiveHealthChecker checker = new GraphHopperActiveHealthChecker(
			restTemplate,
			() -> new GraphHopperEndpointSelection(
				"http://graphhopper-blue.test",
				null,
				"blue",
				null));
		server.expect(requestTo("http://graphhopper-blue.test/healthcheck"))
			.andRespond(withServerError());

		GraphHopperActiveHealthChecker.GraphHopperHealthStatus status = checker.check();

		assertThat(status.status()).isEqualTo("DOWN");
		assertThat(status.activeSlot()).isEqualTo("blue");
		server.verify();
	}
}
