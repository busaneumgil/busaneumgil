package com.ssafy.e102.global.external.kakao;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.e102.global.geo.dto.GeoPointResponse;

@Component
public class KakaoLocalClient {

	private static final String KAKAO_AK_PREFIX = "KakaoAK ";
	private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";

	private final RestTemplate restTemplate;
	private final KakaoLocalProperties properties;

	public KakaoLocalClient(RestTemplateBuilder builder, KakaoLocalProperties properties) {
		this.restTemplate = builder.connectTimeout(Duration.ofSeconds(3))
			.readTimeout(Duration.ofSeconds(3))
			.build();
		this.properties = properties;
	}

	public KakaoPlaceSearchResult searchKeyword(KakaoPlaceSearchRequest request) {
		if (!StringUtils.hasText(properties.apiKey())) {
			throw new RestClientException("Kakao local REST API key is empty.");
		}

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(properties.baseUrl() + KEYWORD_SEARCH_PATH)
			.queryParam("query", request.keyword())
			.queryParam("page", request.page())
			.queryParam("size", request.size());
		if (request.lat() != null && request.lng() != null) {
			uriBuilder.queryParam("y", request.lat())
				.queryParam("x", request.lng());
		}
		if (request.radius() != null && request.lat() != null && request.lng() != null) {
			uriBuilder.queryParam("radius", request.radius());
		}

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, KAKAO_AK_PREFIX + properties.apiKey());
		KakaoKeywordSearchResponse response = restTemplate.exchange(
			uriBuilder.build()
				.encode()
				.toUri(),
			HttpMethod.GET,
			new HttpEntity<>(headers),
			KakaoKeywordSearchResponse.class)
			.getBody();
		if (response == null || response.meta() == null || response.documents() == null) {
			throw new RestClientException("Kakao local response body is empty.");
		}
		return new KakaoPlaceSearchResult(
			response.documents()
				.stream()
				.map(KakaoKeywordSearchDocument::toDomain)
				.toList(),
			response.meta()
				.pageableCount(),
			response.meta()
				.isEnd());
	}

	private record KakaoKeywordSearchResponse(
		KakaoKeywordSearchMeta meta,
		List<KakaoKeywordSearchDocument> documents) {
	}

	private record KakaoKeywordSearchMeta(
		@JsonProperty("pageable_count")
		int pageableCount,
		@JsonProperty("is_end")
		boolean isEnd) {
	}

	private record KakaoKeywordSearchDocument(
		String id,
		@JsonProperty("place_name")
		String placeName,
		@JsonProperty("road_address_name")
		String roadAddressName,
		@JsonProperty("address_name")
		String addressName,
		String x,
		String y,
		String distance) {

		private KakaoPlaceDocument toDomain() {
			return new KakaoPlaceDocument(
				id,
				placeName,
				address(),
				distanceMeter(),
				new GeoPointResponse(Double.parseDouble(y), Double.parseDouble(x)));
		}

		private String address() {
			if (StringUtils.hasText(roadAddressName)) {
				return roadAddressName;
			}
			return addressName;
		}

		private Integer distanceMeter() {
			if (!StringUtils.hasText(distance)) {
				return null;
			}
			return Integer.valueOf(distance);
		}
	}
}
