package com.ssafy.e102.global.external.bims;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

class BusanBimsClientTest {

	@Test
	@DisplayName("BIMS serviceKey의 +와 =를 query-safe encoding으로 변환한다")
	void encodedServiceKeyEscapesPlusAndEquals() {
		BusanBimsClient client = new BusanBimsClient(
			new RestTemplateBuilder(),
			new BusanBimsProperties("https://apis.data.go.kr/6260000/BusanBIMS", "abc+def==",
				Duration.ofSeconds(5), Duration.ofSeconds(5)));

		String encodedServiceKey = ReflectionTestUtils.invokeMethod(client, "encodedServiceKey");

		assertThat(encodedServiceKey).isEqualTo("abc%2Bdef%3D%3D");
	}

	@Test
	@DisplayName("이미 인코딩된 BIMS serviceKey는 중복 인코딩하지 않는다")
	void encodedServiceKeyKeepsAlreadyEncodedValue() {
		BusanBimsClient client = new BusanBimsClient(
			new RestTemplateBuilder(),
			new BusanBimsProperties("https://apis.data.go.kr/6260000/BusanBIMS", "abc%2Bdef%3D%3D",
				Duration.ofSeconds(5), Duration.ofSeconds(5)));

		String encodedServiceKey = ReflectionTestUtils.invokeMethod(client, "encodedServiceKey");

		assertThat(encodedServiceKey).isEqualTo("abc%2Bdef%3D%3D");
	}
}
