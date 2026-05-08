package com.ssafy.e102.global.external.odsay;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

class OdsayClientTest {

	private MockRestServiceServer server;
	private OdsayClient client;

	@BeforeEach
	void setUp() {
		client = new OdsayClient(new RestTemplateBuilder(), new OdsayProperties(
			"https://api.odsay.test/v1/api",
			"test-api-key",
			Duration.ofSeconds(5),
			Duration.ofSeconds(5)));
		RestTemplate restTemplate = (RestTemplate)ReflectionTestUtils.getField(client, "restTemplate");
		server = MockRestServiceServer.bindTo(restTemplate).build();
	}

	@Test
	@DisplayName("ODSay search 응답 body에 error가 있으면 경로 없음이 아니라 EX5020으로 매핑한다")
	void searchMapsOdsayErrorBodyToExternalFailure() {
		server.expect(request -> {}).andRespond(withSuccess("""
			{
			  "error": [
			    {
			      "code": "500",
			      "message": "[ApiKeyAuthFailed] ApiKey authentication failed."
			    }
			  ]
			}
			""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.searchPubTransPath(
			new GeoPointRequest(35.1200, 128.9360),
			new GeoPointRequest(35.1315, 128.8823)))
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED);
		server.verify();
	}

	@Test
	@DisplayName("ODSay loadLane 응답 body에 error가 있으면 경로 없음이 아니라 EX5020으로 매핑한다")
	void loadLaneMapsOdsayErrorBodyToExternalFailure() {
		server.expect(request -> {}).andRespond(withSuccess("""
			{
			  "error": [
			    {
			      "code": "429",
			      "message": "quota exceeded"
			    }
			  ]
			}
			""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.loadLane("1:2:3"))
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED);
		server.verify();
	}
}
