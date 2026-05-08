package com.ssafy.e102.global.external.kakao;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

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
	private static final String COORD_TO_ADDRESS_PATH = "/v2/local/geo/coord2address.json";
	private static final String WGS84 = "WGS84";

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

	public Optional<KakaoAddressDocument> reverseGeocode(double lat, double lng) {
		if (!StringUtils.hasText(properties.apiKey())) {
			throw new RestClientException("Kakao local REST API key is empty.");
		}

		UriComponentsBuilder uriBuilder = UriComponentsBuilder
			.fromUriString(properties.baseUrl() + COORD_TO_ADDRESS_PATH)
			.queryParam("x", lng)
			.queryParam("y", lat)
			.queryParam("input_coord", WGS84);

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, KAKAO_AK_PREFIX + properties.apiKey());
		KakaoCoordToAddressResponse response = restTemplate.exchange(
			uriBuilder.build()
				.encode()
				.toUri(),
			HttpMethod.GET,
			new HttpEntity<>(headers),
			KakaoCoordToAddressResponse.class)
			.getBody();
		if (response == null || response.meta() == null || response.documents() == null) {
			throw new RestClientException("Kakao local response body is empty.");
		}
		return response.documents()
			.stream()
			.filter(document -> document != null)
			.map(KakaoCoordToAddressDocument::toDomain)
			.filter(document -> StringUtils.hasText(document.displayAddress()))
			.findFirst();
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

	private record KakaoCoordToAddressResponse(
		KakaoCoordToAddressMeta meta,
		List<KakaoCoordToAddressDocument> documents) {
	}

	private record KakaoCoordToAddressMeta(
		@JsonProperty("total_count")
		int totalCount) {
	}

	private record KakaoCoordToAddressDocument(
		KakaoAddress address,
		@JsonProperty("road_address")
		KakaoRoadAddress roadAddress) {

		private KakaoAddressDocument toDomain() {
			return new KakaoAddressDocument(
				addressName(),
				roadAddressName(),
				region1DepthName(),
				region2DepthName(),
				region3DepthName());
		}

		private String addressName() {
			if (address == null) {
				return null;
			}
			return address.addressName();
		}

		private String roadAddressName() {
			if (roadAddress == null) {
				return null;
			}
			return roadAddress.addressName();
		}

		private String region1DepthName() {
			if (roadAddress != null && StringUtils.hasText(roadAddress.region1DepthName())) {
				return roadAddress.region1DepthName();
			}
			return address == null ? null : address.region1DepthName();
		}

		private String region2DepthName() {
			if (roadAddress != null && StringUtils.hasText(roadAddress.region2DepthName())) {
				return roadAddress.region2DepthName();
			}
			return address == null ? null : address.region2DepthName();
		}

		private String region3DepthName() {
			if (roadAddress != null && StringUtils.hasText(roadAddress.region3DepthName())) {
				return roadAddress.region3DepthName();
			}
			return address == null ? null : address.region3DepthName();
		}
	}

	private record KakaoAddress(
		@JsonProperty("address_name")
		String addressName,
		@JsonProperty("region_1depth_name")
		String region1DepthName,
		@JsonProperty("region_2depth_name")
		String region2DepthName,
		@JsonProperty("region_3depth_name")
		String region3DepthName) {
	}

	private record KakaoRoadAddress(
		@JsonProperty("address_name")
		String addressName,
		@JsonProperty("region_1depth_name")
		String region1DepthName,
		@JsonProperty("region_2depth_name")
		String region2DepthName,
		@JsonProperty("region_3depth_name")
		String region3DepthName) {
	}
}
