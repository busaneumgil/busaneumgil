package com.ssafy.e102.domain.place.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import com.ssafy.e102.domain.place.dto.response.PlaceDetailResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceListResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceMarkerResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceSearchItemResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceSearchResponse;
import com.ssafy.e102.domain.place.entity.Place;
import com.ssafy.e102.domain.place.exception.PlaceErrorCode;
import com.ssafy.e102.domain.place.exception.PlaceException;
import com.ssafy.e102.domain.place.repository.BookmarkRepository;
import com.ssafy.e102.domain.place.repository.PlaceRepository;
import com.ssafy.e102.domain.place.type.AccessibilityFeatureType;
import com.ssafy.e102.domain.place.type.PlaceCategory;
import com.ssafy.e102.global.external.kakao.KakaoLocalClient;
import com.ssafy.e102.global.external.kakao.KakaoPlaceDocument;
import com.ssafy.e102.global.external.kakao.KakaoPlaceSearchRequest;
import com.ssafy.e102.global.external.kakao.KakaoPlaceSearchResult;
import com.ssafy.e102.global.geo.GeoPointConverter;

@Service
@Transactional(readOnly = true)
public class PlaceService {

	private static final int DEFAULT_SEARCH_PAGE = 0;
	private static final int DEFAULT_SEARCH_SIZE = 10;
	private static final int MIN_PAGE = 0;
	private static final int MAX_SEARCH_SIZE = 15;
	private static final String EMPTY_FILTER_SENTINEL = "__EMPTY_FILTER__";
	private final PlaceRepository placeRepository;
	private final BookmarkRepository bookmarkRepository;
	private final KakaoLocalClient kakaoLocalClient;
	private final GeoPointConverter geoPointConverter;

	public PlaceService(
		PlaceRepository placeRepository,
		BookmarkRepository bookmarkRepository,
		KakaoLocalClient kakaoLocalClient,
		GeoPointConverter geoPointConverter) {
		this.placeRepository = placeRepository;
		this.bookmarkRepository = bookmarkRepository;
		this.kakaoLocalClient = kakaoLocalClient;
		this.geoPointConverter = geoPointConverter;
	}

	public PlaceSearchResponse searchPlaces(
		String keyword,
		String lat,
		String lng,
		String radius,
		String page,
		String size) {
		String normalizedKeyword = normalizeKeyword(keyword);
		Double parsedLat = parseOptionalDouble(lat);
		Double parsedLng = parseOptionalDouble(lng);
		Integer parsedRadius = parseOptionalPositiveInteger(radius);
		validateSearchCoordinateCondition(parsedLat, parsedLng, parsedRadius);
		int parsedPage = parsePositiveIntegerOrDefault(page, DEFAULT_SEARCH_PAGE);
		int parsedSize = parseIntegerOrDefault(size, DEFAULT_SEARCH_SIZE);
		validateSearchSize(parsedSize);

		try {
			KakaoPlaceSearchResult kakaoResult = kakaoLocalClient.searchKeyword(new KakaoPlaceSearchRequest(
				normalizedKeyword,
				parsedLat,
				parsedLng,
				parsedRadius,
				parsedPage + 1,
				parsedSize));
			Map<String, Place> matchedPlaces = getMatchedPlaces(kakaoResult.documents());
			return new PlaceSearchResponse(
				kakaoResult.documents()
					.stream()
					.map(place -> PlaceSearchItemResponse.of(place, matchedPlaces))
					.toList(),
				parsedPage,
				parsedSize,
				kakaoResult.totalElements(),
				calculateTotalPages(kakaoResult.totalElements(), parsedSize),
				!kakaoResult.isEnd());
		} catch (RestClientException | IllegalArgumentException exception) {
			throw new PlaceException(PlaceErrorCode.PLACE_SEARCH_EXTERNAL_API_FAILED);
		}
	}

	public PlaceListResponse getPlaces(
		UUID userId,
		String lat,
		String lng,
		String radius,
		String category,
		String featureType) {
		double parsedLat = parseRequiredDouble(lat);
		double parsedLng = parseRequiredDouble(lng);
		Integer parsedRadius = parseOptionalPositiveInteger(radius);
		Set<PlaceCategory> categories = parseCsvEnums(category, PlaceCategory.class);
		Set<AccessibilityFeatureType> featureTypes = parseCsvEnums(featureType, AccessibilityFeatureType.class);

		List<Place> places = placeRepository.findPlaceMarkers(
			parsedLat,
			parsedLng,
			parsedRadius,
			toEnumNames(categories),
			categories.isEmpty(),
			toEnumNames(featureTypes),
			featureTypes.isEmpty());
		Set<Long> bookmarkedPlaceIds = getBookmarkedPlaceIds(userId, places);
		return new PlaceListResponse(places.stream()
			.map(place -> PlaceMarkerResponse.of(place, bookmarkedPlaceIds, geoPointConverter))
			.toList());
	}

	public PlaceDetailResponse getPlace(UUID userId, String placeId) {
		Long parsedPlaceId = parsePositiveLong(placeId);
		Place place = placeRepository.findWithAccessibilityFeaturesByPlaceId(parsedPlaceId)
			.orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND));
		boolean isBookmarked = bookmarkRepository.existsByUser_UserIdAndPlace_PlaceId(userId, parsedPlaceId);
		return PlaceDetailResponse.of(place, isBookmarked, geoPointConverter);
	}

	private Map<String, Place> getMatchedPlaces(List<KakaoPlaceDocument> kakaoPlaces) {
		List<String> providerPlaceIds = kakaoPlaces.stream()
			.map(KakaoPlaceDocument::id)
			.toList();
		if (providerPlaceIds.isEmpty()) {
			return Map.of();
		}
		return placeRepository.findAllByProviderPlaceIdIn(providerPlaceIds)
			.stream()
			.collect(Collectors.toMap(Place::getProviderPlaceId, Function.identity(), (first, second) -> first));
	}

	private Set<Long> getBookmarkedPlaceIds(UUID userId, List<Place> places) {
		List<Long> placeIds = places.stream()
			.map(Place::getPlaceId)
			.toList();
		if (placeIds.isEmpty()) {
			return Set.of();
		}
		return bookmarkRepository.findBookmarkedPlaceIds(userId, placeIds);
	}

	private String normalizeKeyword(String keyword) {
		if (!StringUtils.hasText(keyword)) {
			throw new PlaceException(PlaceErrorCode.PLACE_KEYWORD_REQUIRED);
		}
		return keyword.trim();
	}

	private void validateSearchCoordinateCondition(Double lat, Double lng, Integer radius) {
		if ((lat == null) != (lng == null)) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
		if (radius != null && (lat == null || lng == null)) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
	}

	private double parseRequiredDouble(String value) {
		if (!StringUtils.hasText(value)) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
		return parseDouble(value);
	}

	private Double parseOptionalDouble(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return parseDouble(value);
	}

	private double parseDouble(String value) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException exception) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
	}

	private Integer parseOptionalPositiveInteger(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		int parsedValue = parseInteger(value);
		if (parsedValue <= 0) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
		return parsedValue;
	}

	private int parsePositiveIntegerOrDefault(String value, int defaultValue) {
		int parsedValue = parseIntegerOrDefault(value, defaultValue);
		if (parsedValue < MIN_PAGE) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
		return parsedValue;
	}

	private int parseIntegerOrDefault(String value, int defaultValue) {
		if (!StringUtils.hasText(value)) {
			return defaultValue;
		}
		return parseInteger(value);
	}

	private int parseInteger(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
	}

	private void validateSearchSize(int size) {
		if (size < 1 || size > MAX_SEARCH_SIZE) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
	}

	private Long parsePositiveLong(String value) {
		try {
			long parsedValue = Long.parseLong(value);
			if (parsedValue <= 0) {
				throw new NumberFormatException("placeId must be positive.");
			}
			return parsedValue;
		} catch (NumberFormatException exception) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
	}

	private <E extends Enum<E>> Set<E> parseCsvEnums(String value, Class<E> enumType) {
		if (!StringUtils.hasText(value)) {
			return Set.of();
		}
		try {
			return Arrays.stream(value.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.map(token -> Enum.valueOf(enumType, token))
				.collect(Collectors.toSet());
		} catch (IllegalArgumentException exception) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
	}

	private <E extends Enum<E>> Set<String> toEnumNames(Set<E> values) {
		if (values.isEmpty()) {
			return Set.of(EMPTY_FILTER_SENTINEL);
		}
		return values.stream()
			.map(Enum::name)
			.collect(Collectors.toSet());
	}

	private int calculateTotalPages(long totalElements, int size) {
		return (int)Math.ceil((double)totalElements / size);
	}
}
