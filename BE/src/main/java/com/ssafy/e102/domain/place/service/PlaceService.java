package com.ssafy.e102.domain.place.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import com.ssafy.e102.domain.place.dto.request.PlaceClickDetailRequest;
import com.ssafy.e102.domain.place.dto.response.PlaceAccessibilityFeatureResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceClickDetailResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceDetailResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceListResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceMarkerResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceReverseGeocodeResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceSearchItemResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceSearchResponse;
import com.ssafy.e102.domain.place.entity.Place;
import com.ssafy.e102.domain.place.exception.PlaceErrorCode;
import com.ssafy.e102.domain.place.exception.PlaceException;
import com.ssafy.e102.domain.place.repository.BookmarkRepository;
import com.ssafy.e102.domain.place.repository.PlaceRepository;
import com.ssafy.e102.domain.place.support.BookmarkTargetIdFactory;
import com.ssafy.e102.domain.place.type.AccessibilityFeatureType;
import com.ssafy.e102.domain.place.type.PlaceCategory;
import com.ssafy.e102.domain.place.type.PlaceClickType;
import com.ssafy.e102.domain.place.type.PlaceDetailType;
import com.ssafy.e102.global.external.kakao.KakaoAddressDocument;
import com.ssafy.e102.global.external.kakao.KakaoLocalClient;
import com.ssafy.e102.global.external.kakao.KakaoPlaceDocument;
import com.ssafy.e102.global.external.kakao.KakaoPlaceSearchRequest;
import com.ssafy.e102.global.external.kakao.KakaoPlaceSearchResult;
import com.ssafy.e102.global.geo.GeoPointConverter;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PlaceService {

	private static final int DEFAULT_KAKAO_SEARCH_PAGE = 1;
	private static final int DEFAULT_SEARCH_SIZE = 10;
	private static final int MAX_SEARCH_SIZE = 15;
	private static final int DEFAULT_PLACE_RADIUS_METER = 1000;
	private static final int DEFAULT_PLACE_DETAIL_RADIUS_METER = 300;
	private static final int DEFAULT_PLACE_DETAIL_SIZE = 5;
	private static final int MAX_PLACE_RADIUS_METER = 3000;
	private static final int PLACE_MARKER_LIMIT = 200;
	private static final double MIN_LAT = -90.0;
	private static final double MAX_LAT = 90.0;
	private static final double MIN_LNG = -180.0;
	private static final double MAX_LNG = 180.0;
	private static final String DEFAULT_PROVIDER = "KAKAO";
	private static final String SEARCH_CURSOR_PREFIX = "kakao:";
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
		String cursor,
		String size) {
		String normalizedKeyword = normalizeKeyword(keyword);
		Double parsedLat = parseOptionalDouble(lat);
		Double parsedLng = parseOptionalDouble(lng);
		Integer parsedRadius = parseOptionalPositiveInteger(radius);
		validateSearchCoordinateCondition(parsedLat, parsedLng, parsedRadius);
		int kakaoPage = parseSearchCursor(cursor);
		int parsedSize = parseIntegerOrDefault(size, DEFAULT_SEARCH_SIZE);
		validateSearchSize(parsedSize);

		try {
			KakaoPlaceSearchResult kakaoResult = kakaoLocalClient.searchKeyword(new KakaoPlaceSearchRequest(
				normalizedKeyword,
				parsedLat,
				parsedLng,
				parsedRadius,
				kakaoPage,
				parsedSize));
			Map<String, Place> matchedPlaces = getMatchedPlaces(kakaoResult.documents());
			boolean hasNext = !kakaoResult.isEnd();
			return new PlaceSearchResponse(
				kakaoResult.documents()
					.stream()
					.map(place -> PlaceSearchItemResponse.of(place, matchedPlaces))
					.toList(),
				hasNext ? encodeSearchCursor(kakaoPage + 1) : null,
				parsedSize,
				kakaoResult.totalElements(),
				hasNext);
		} catch (RestClientException | IllegalArgumentException exception) {
			log.warn("장소 검색 외부 API 호출 실패. keyword={}, kakaoPage={}, size={}",
				normalizedKeyword,
				kakaoPage,
				parsedSize,
				exception);
			throw new PlaceException(PlaceErrorCode.PLACE_SEARCH_EXTERNAL_API_FAILED, exception);
		}
	}

	public PlaceReverseGeocodeResponse reverseGeocode(String lat, String lng) {
		double parsedLat = parseRequiredDouble(lat);
		double parsedLng = parseRequiredDouble(lng);
		validateCoordinateRange(parsedLat, parsedLng);

		try {
			KakaoAddressDocument addressDocument = kakaoLocalClient.reverseGeocode(parsedLat, parsedLng)
				.orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_ADDRESS_NOT_FOUND));
			return PlaceReverseGeocodeResponse.from(addressDocument);
		} catch (RestClientException | IllegalArgumentException exception) {
			log.warn("좌표 주소 변환 외부 API 호출 실패. lat={}, lng={}", parsedLat, parsedLng, exception);
			throw new PlaceException(PlaceErrorCode.PLACE_REVERSE_GEOCODE_EXTERNAL_API_FAILED, exception);
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
		int parsedRadius = parsePlaceRadius(radius);
		Set<PlaceCategory> categories = parseCsvEnums(category, PlaceCategory.class);
		Set<AccessibilityFeatureType> featureTypes = parseCsvEnums(featureType, AccessibilityFeatureType.class);

		List<Long> placeIds = placeRepository.findPlaceMarkerIds(
			parsedLat,
			parsedLng,
			parsedRadius,
			toEnumNames(categories),
			categories.isEmpty(),
			toEnumNames(featureTypes),
			featureTypes.isEmpty(),
			PLACE_MARKER_LIMIT);
		if (placeIds.isEmpty()) {
			return new PlaceListResponse(List.of());
		}
		List<Place> places = findPlacesWithAccessibilityFeatures(placeIds);
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

	public PlaceClickDetailResponse getPlaceDetail(UUID userId, PlaceClickDetailRequest request) {
		validateBasePlaceClickDetailRequest(request);
		Optional<Place> internalPlace = findMatchedInternalPlace(request.providerPlaceId());
		if (internalPlace.isPresent()) {
			return toInternalClickDetailResponse(userId, request, internalPlace.get());
		}
		if (request.clickType() == PlaceClickType.POI) {
			validateExternalPoiDetailRequest(request);
			return getExternalPoiDetail(userId, request);
		}
		return getExternalAddressDetail(userId, request);
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

	private List<Place> findPlacesWithAccessibilityFeatures(List<Long> placeIds) {
		Map<Long, Place> placesById = placeRepository.findAllByPlaceIdIn(placeIds)
			.stream()
			.collect(Collectors.toMap(Place::getPlaceId, Function.identity(), (first, second) -> first));
		return placeIds.stream()
			.map(placesById::get)
			.filter(place -> place != null)
			.toList();
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

	private int parseSearchCursor(String cursor) {
		if (!StringUtils.hasText(cursor)) {
			return DEFAULT_KAKAO_SEARCH_PAGE;
		}
		try {
			String decodedCursor = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
			if (!decodedCursor.startsWith(SEARCH_CURSOR_PREFIX)) {
				throw new NumberFormatException("장소 검색 cursor prefix가 올바르지 않습니다.");
			}
			int kakaoPage = Integer.parseInt(decodedCursor.substring(SEARCH_CURSOR_PREFIX.length()));
			if (kakaoPage < DEFAULT_KAKAO_SEARCH_PAGE) {
				throw new NumberFormatException("장소 검색 cursor는 양수여야 합니다.");
			}
			return kakaoPage;
		} catch (IllegalArgumentException exception) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
	}

	private String encodeSearchCursor(int kakaoPage) {
		return Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString((SEARCH_CURSOR_PREFIX + kakaoPage).getBytes(StandardCharsets.UTF_8));
	}

	private String normalizeKeyword(String keyword) {
		if (!StringUtils.hasText(keyword)) {
			throw new PlaceException(PlaceErrorCode.PLACE_KEYWORD_REQUIRED);
		}
		return keyword.trim();
	}

	private void validateBasePlaceClickDetailRequest(PlaceClickDetailRequest request) {
		if (request == null || request.lat() == null || request.lng() == null || request.clickType() == null) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
		validateCoordinateRange(request.lat(), request.lng());
	}

	private void validateExternalPoiDetailRequest(PlaceClickDetailRequest request) {
		if (!StringUtils.hasText(request.nameHint())) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
	}

	private Optional<Place> findMatchedInternalPlace(String providerPlaceId) {
		if (!StringUtils.hasText(providerPlaceId)) {
			return Optional.empty();
		}
		return placeRepository.findAllByProviderPlaceIdIn(List.of(providerPlaceId.trim()))
			.stream()
			.findFirst();
	}

	private PlaceClickDetailResponse toInternalClickDetailResponse(UUID userId, PlaceClickDetailRequest request,
		Place place) {
		String bookmarkTargetId = BookmarkTargetIdFactory.fromInternalPlace(place.getPlaceId());
		return new PlaceClickDetailResponse(
			bookmarkTargetId,
			PlaceDetailType.INTERNAL_PLACE,
			place.getPlaceId(),
			normalizeProvider(request.provider(), place.getProviderPlaceId()),
			place.getProviderPlaceId(),
			place.getName(),
			place.getCategory(),
			null,
			place.getAddress(),
			geoPointConverter.toResponse(place.getPoint()),
			place.getAccessibilityFeatures().stream()
				.map(PlaceAccessibilityFeatureResponse::from)
				.toList(),
			isPlaceBookmarked(userId, place.getPlaceId(), bookmarkTargetId));
	}

	private PlaceClickDetailResponse getExternalPoiDetail(UUID userId, PlaceClickDetailRequest request) {
		try {
			KakaoPlaceSearchResult result = kakaoLocalClient.searchKeyword(new KakaoPlaceSearchRequest(
				request.nameHint().trim(),
				request.lat(),
				request.lng(),
				DEFAULT_PLACE_DETAIL_RADIUS_METER,
				DEFAULT_KAKAO_SEARCH_PAGE,
				DEFAULT_PLACE_DETAIL_SIZE));
			KakaoPlaceDocument selected = selectPoiCandidate(request, result.documents())
				.orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_CLICK_DETAIL_NOT_FOUND));
			String provider = normalizeProvider(request.provider(), selected.id());
			String bookmarkTargetId = BookmarkTargetIdFactory.fromExternalPoi(
				provider,
				selected.id(),
				selected.placeName(),
				selected.point().lat(),
				selected.point().lng());
			return new PlaceClickDetailResponse(
				bookmarkTargetId,
				PlaceDetailType.EXTERNAL_POI,
				null,
				provider,
				selected.id(),
				selected.placeName(),
				null,
				selected.providerCategory(),
				selected.address(),
				selected.point(),
				List.of(),
				bookmarkRepository.existsByUser_UserIdAndBookmarkTargetId(userId, bookmarkTargetId));
		} catch (RestClientException | IllegalArgumentException exception) {
			log.warn("지도 클릭 상세 POI 외부 API 호출 실패. request={}", request, exception);
			throw new PlaceException(PlaceErrorCode.PLACE_SEARCH_EXTERNAL_API_FAILED, exception);
		}
	}

	private Optional<KakaoPlaceDocument> selectPoiCandidate(
		PlaceClickDetailRequest request,
		List<KakaoPlaceDocument> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			return Optional.empty();
		}
		if (StringUtils.hasText(request.providerPlaceId())) {
			Optional<KakaoPlaceDocument> exactProviderMatch = candidates.stream()
				.filter(candidate -> request.providerPlaceId().trim().equals(candidate.id()))
				.findFirst();
			if (exactProviderMatch.isPresent()) {
				return exactProviderMatch;
			}
		}
		String normalizedName = normalizeComparableText(request.nameHint());
		Optional<KakaoPlaceDocument> exactNameMatch = candidates.stream()
			.filter(candidate -> normalizedName.equals(normalizeComparableText(candidate.placeName())))
			.findFirst();
		if (exactNameMatch.isPresent()) {
			return exactNameMatch;
		}
		return candidates.stream()
			.min(Comparator.comparing(KakaoPlaceDocument::distanceMeter, Comparator.nullsLast(Integer::compareTo)));
	}

	private PlaceClickDetailResponse getExternalAddressDetail(UUID userId, PlaceClickDetailRequest request) {
		try {
			KakaoAddressDocument addressDocument = kakaoLocalClient.reverseGeocode(request.lat(), request.lng())
				.orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_ADDRESS_NOT_FOUND));
			String displayAddress = addressDocument.displayAddress();
			String provider = normalizeProvider(request.provider(), null);
			String bookmarkTargetId = BookmarkTargetIdFactory.fromExternalAddress(
				provider,
				displayAddress,
				request.lat(),
				request.lng(),
				displayAddress);
			return new PlaceClickDetailResponse(
				bookmarkTargetId,
				PlaceDetailType.EXTERNAL_ADDRESS,
				null,
				provider,
				null,
				displayAddress,
				null,
				null,
				displayAddress,
				geoPointConverter
					.toResponse(geoPointConverter.toPoint(new GeoPointRequest(request.lat(), request.lng()))),
				List.of(),
				bookmarkRepository.existsByUser_UserIdAndBookmarkTargetId(userId, bookmarkTargetId));
		} catch (RestClientException | IllegalArgumentException exception) {
			log.warn("지도 클릭 상세 주소 외부 API 호출 실패. request={}", request, exception);
			throw new PlaceException(PlaceErrorCode.PLACE_REVERSE_GEOCODE_EXTERNAL_API_FAILED, exception);
		}
	}

	private boolean isPlaceBookmarked(UUID userId, Long placeId, String bookmarkTargetId) {
		if (bookmarkRepository.existsByUser_UserIdAndPlace_PlaceId(userId, placeId)) {
			return true;
		}
		return bookmarkRepository.existsByUser_UserIdAndBookmarkTargetId(userId, bookmarkTargetId);
	}

	private String normalizeProvider(String requestedProvider, String providerPlaceId) {
		if (StringUtils.hasText(requestedProvider)) {
			return requestedProvider.trim().toUpperCase();
		}
		if (StringUtils.hasText(providerPlaceId)) {
			return DEFAULT_PROVIDER;
		}
		return null;
	}

	private String normalizeComparableText(String value) {
		if (!StringUtils.hasText(value)) {
			return "";
		}
		return value.trim().replaceAll("\\s+", " ");
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

	private void validateCoordinateRange(double lat, double lng) {
		if (lat < MIN_LAT || lat > MAX_LAT || lng < MIN_LNG || lng > MAX_LNG) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
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

	private int parsePlaceRadius(String value) {
		if (!StringUtils.hasText(value)) {
			return DEFAULT_PLACE_RADIUS_METER;
		}
		int parsedRadius = parseOptionalPositiveInteger(value);
		if (parsedRadius > MAX_PLACE_RADIUS_METER) {
			throw new PlaceException(PlaceErrorCode.INVALID_PLACE_REQUEST);
		}
		return parsedRadius;
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
				throw new NumberFormatException("placeId는 양수여야 합니다.");
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
}
