package com.ssafy.e102.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import com.ssafy.e102.domain.place.dto.request.PlaceClickDetailRequest;
import com.ssafy.e102.domain.place.dto.response.PlaceClickDetailResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceDetailResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceListResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceReverseGeocodeResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceSearchResponse;
import com.ssafy.e102.domain.place.entity.Place;
import com.ssafy.e102.domain.place.entity.PlaceAccessibilityFeature;
import com.ssafy.e102.domain.place.exception.PlaceErrorCode;
import com.ssafy.e102.domain.place.exception.PlaceException;
import com.ssafy.e102.domain.place.repository.BookmarkRepository;
import com.ssafy.e102.domain.place.repository.PlaceRepository;
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
import com.ssafy.e102.global.geo.dto.GeoPointResponse;

class PlaceServiceTest {

	@Mock
	private PlaceRepository placeRepository;

	@Mock
	private BookmarkRepository bookmarkRepository;

	@Mock
	private KakaoLocalClient kakaoLocalClient;

	private PlaceService placeService;
	private GeoPointConverter geoPointConverter;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		geoPointConverter = new GeoPointConverter();
		placeService = new PlaceService(placeRepository, bookmarkRepository, kakaoLocalClient, geoPointConverter);
	}

	@Test
	@DisplayName("장소 검색은 카카오 결과를 내부 장소와 providerPlaceId로 매칭한다")
	void searchPlaces() {
		KakaoPlaceDocument kakaoPlace = new KakaoPlaceDocument(
			"123456789",
			"부산시민공원",
			"부산광역시 부산진구 시민공원로 73",
			"여행 > 관광,명소 > 공원",
			350,
			new GeoPointResponse(35.1686, 129.0576));
		Place matchedPlace = place(
			10L,
			"부산시민공원",
			PlaceCategory.TOURIST_SPOT,
			"123456789",
			35.1686,
			129.0576,
			AccessibilityFeatureType.accessibleEntrance);
		when(kakaoLocalClient.searchKeyword(new KakaoPlaceSearchRequest(
			"부산시민공원",
			35.1686,
			129.0576,
			1000,
			1,
			10)))
			.thenReturn(new KakaoPlaceSearchResult(List.of(kakaoPlace), 1, true));
		when(placeRepository.findAllByProviderPlaceIdIn(List.of("123456789")))
			.thenReturn(List.of(matchedPlace));

		PlaceSearchResponse response = placeService.searchPlaces(
			" 부산시민공원 ",
			"35.1686",
			"129.0576",
			"1000",
			null,
			"10");

		assertThat(response.nextCursor()).isNull();
		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.hasNext()).isFalse();
		assertThat(response.places()).hasSize(1);
		assertThat(response.places().get(0).placeId()).isEqualTo(10L);
		assertThat(response.places().get(0).matched()).isTrue();
		assertThat(response.places().get(0).category()).isEqualTo(PlaceCategory.TOURIST_SPOT);
		assertThat(response.places().get(0).accessibilityFeatures()).hasSize(1);
	}

	@Test
	@DisplayName("장소 검색은 다음 cursor로 카카오 다음 페이지를 조회한다")
	void searchPlacesWithCursor() {
		when(kakaoLocalClient.searchKeyword(new KakaoPlaceSearchRequest(
			"부산시민공원",
			null,
			null,
			null,
			1,
			10)))
			.thenReturn(new KakaoPlaceSearchResult(List.of(), 30, false));

		PlaceSearchResponse firstResponse = placeService.searchPlaces("부산시민공원", null, null, null, null, "10");

		when(kakaoLocalClient.searchKeyword(new KakaoPlaceSearchRequest(
			"부산시민공원",
			null,
			null,
			null,
			2,
			10)))
			.thenReturn(new KakaoPlaceSearchResult(List.of(), 30, true));

		PlaceSearchResponse secondResponse = placeService.searchPlaces(
			"부산시민공원",
			null,
			null,
			null,
			firstResponse.nextCursor(),
			"10");

		assertThat(firstResponse.hasNext()).isTrue();
		assertThat(firstResponse.nextCursor()).isNotBlank();
		assertThat(secondResponse.hasNext()).isFalse();
		assertThat(secondResponse.nextCursor()).isNull();
	}

	@Test
	@DisplayName("좌표 주소 변환은 카카오 주소 결과를 응답으로 매핑한다")
	void reverseGeocode() {
		when(kakaoLocalClient.reverseGeocode(35.1686, 129.0576))
			.thenReturn(Optional.of(new KakaoAddressDocument(
				"부산 부산진구 범전동 200",
				"부산 부산진구 시민공원로 73",
				"부산",
				"부산진구",
				"범전동")));

		PlaceReverseGeocodeResponse response = placeService.reverseGeocode("35.1686", "129.0576");

		assertThat(response.displayAddress()).isEqualTo("부산 부산진구 시민공원로 73");
		assertThat(response.roadAddress()).isEqualTo("부산 부산진구 시민공원로 73");
		assertThat(response.address()).isEqualTo("부산 부산진구 범전동 200");
	}

	@Test
	@DisplayName("외부 상세 조회는 providerPlaceId가 내부 장소와 매칭되면 canonical 내부 장소를 반환한다")
	void getPlaceDetailWithInternalProviderMatch() {
		UUID userId = UUID.randomUUID();
		Place place = place(
			10L,
			"부산시민공원",
			PlaceCategory.TOURIST_SPOT,
			"123456789",
			35.1686,
			129.0576,
			AccessibilityFeatureType.accessibleEntrance);
		PlaceClickDetailRequest request = new PlaceClickDetailRequest(
			35.1686,
			129.0576,
			PlaceClickType.POI,
			"KAKAO",
			"123456789",
			"부산시민공원");
		when(placeRepository.findAllByProviderPlaceIdIn(List.of("123456789"))).thenReturn(List.of(place));
		when(bookmarkRepository.existsByUser_UserIdAndBookmarkTargetId(eq(userId), anyString())).thenReturn(true);

		PlaceClickDetailResponse response = placeService.getPlaceDetail(userId, request);

		assertThat(response.detailType()).isEqualTo(PlaceDetailType.INTERNAL_PLACE);
		assertThat(response.bookmarkTargetId()).isNotBlank();
		assertThat(response.placeId()).isEqualTo(10L);
		assertThat(response.name()).isEqualTo("부산시민공원");
		assertThat(response.category()).isEqualTo(PlaceCategory.TOURIST_SPOT);
		assertThat(response.providerCategory()).isNull();
		assertThat(response.isBookmarked()).isTrue();
	}

	@Test
	@DisplayName("외부 상세 조회는 providerPlaceId가 내부 장소와 매칭되면 nameHint 없이도 canonical 내부 장소를 반환한다")
	void getPlaceDetailWithInternalProviderMatchWithoutNameHint() {
		UUID userId = UUID.randomUUID();
		Place place = place(
			10L,
			"부산시민공원",
			PlaceCategory.TOURIST_SPOT,
			"123456789",
			35.1686,
			129.0576,
			AccessibilityFeatureType.accessibleEntrance);
		PlaceClickDetailRequest request = new PlaceClickDetailRequest(
			35.1686,
			129.0576,
			PlaceClickType.POI,
			"KAKAO",
			"123456789",
			null);
		when(placeRepository.findAllByProviderPlaceIdIn(List.of("123456789"))).thenReturn(List.of(place));
		when(bookmarkRepository.existsByUser_UserIdAndBookmarkTargetId(eq(userId), anyString())).thenReturn(false);

		PlaceClickDetailResponse response = placeService.getPlaceDetail(userId, request);

		assertThat(response.detailType()).isEqualTo(PlaceDetailType.INTERNAL_PLACE);
		assertThat(response.placeId()).isEqualTo(10L);
		assertThat(response.name()).isEqualTo("부산시민공원");
	}

	@Test
	@DisplayName("외부 상세 조회는 POI 클릭을 카카오 keyword search로 보강한다")
	void getPlaceDetailForExternalPoi() {
		UUID userId = UUID.randomUUID();
		PlaceClickDetailRequest request = new PlaceClickDetailRequest(
			35.1686,
			129.0576,
			PlaceClickType.POI,
			"KAKAO",
			null,
			"부산시민공원");
		KakaoPlaceDocument kakaoPlace = new KakaoPlaceDocument(
			"123456789",
			"부산시민공원",
			"부산광역시 부산진구 시민공원로 73",
			"여행 > 관광,명소 > 공원",
			12,
			new GeoPointResponse(35.1686, 129.0576));
		when(kakaoLocalClient.searchKeyword(new KakaoPlaceSearchRequest(
			"부산시민공원",
			35.1686,
			129.0576,
			300,
			1,
			5)))
			.thenReturn(new KakaoPlaceSearchResult(List.of(kakaoPlace), 1, true));
		when(bookmarkRepository.existsByUser_UserIdAndBookmarkTargetId(eq(userId), anyString())).thenReturn(false);

		PlaceClickDetailResponse response = placeService.getPlaceDetail(userId, request);

		assertThat(response.detailType()).isEqualTo(PlaceDetailType.EXTERNAL_POI);
		assertThat(response.bookmarkTargetId()).isNotBlank();
		assertThat(response.placeId()).isNull();
		assertThat(response.provider()).isEqualTo("KAKAO");
		assertThat(response.providerPlaceId()).isEqualTo("123456789");
		assertThat(response.providerCategory()).isEqualTo("여행 > 관광,명소 > 공원");
		assertThat(response.category()).isNull();
		assertThat(response.address()).isEqualTo("부산광역시 부산진구 시민공원로 73");
		assertThat(response.isBookmarked()).isFalse();
	}

	@Test
	@DisplayName("외부 상세 조회는 ADDRESS 클릭을 reverse geocode로 보강한다")
	void getPlaceDetailForAddress() {
		UUID userId = UUID.randomUUID();
		PlaceClickDetailRequest request = new PlaceClickDetailRequest(
			35.1686,
			129.0576,
			PlaceClickType.ADDRESS,
			"KAKAO",
			null,
			null);
		when(kakaoLocalClient.reverseGeocode(35.1686, 129.0576))
			.thenReturn(Optional.of(new KakaoAddressDocument(
				"부산 부산진구 범전동 200",
				"부산 부산진구 시민공원로 73",
				"부산",
				"부산진구",
				"범전동")));
		when(bookmarkRepository.existsByUser_UserIdAndBookmarkTargetId(eq(userId), anyString())).thenReturn(false);

		PlaceClickDetailResponse response = placeService.getPlaceDetail(userId, request);

		assertThat(response.detailType()).isEqualTo(PlaceDetailType.EXTERNAL_ADDRESS);
		assertThat(response.bookmarkTargetId()).isNotBlank();
		assertThat(response.placeId()).isNull();
		assertThat(response.name()).isEqualTo("부산 부산진구 시민공원로 73");
		assertThat(response.address()).isEqualTo("부산 부산진구 시민공원로 73");
		assertThat(response.providerCategory()).isNull();
		assertThat(response.isBookmarked()).isFalse();
	}

	@Test
	@DisplayName("외부 상세 조회는 POI 후보를 찾지 못하면 주소 상세로 fallback한다")
	void getPlaceDetailPoiNotFoundFallsBackToAddress() {
		UUID userId = UUID.randomUUID();
		PlaceClickDetailRequest request = new PlaceClickDetailRequest(
			35.1686,
			129.0576,
			PlaceClickType.POI,
			"KAKAO",
			null,
			"없는장소");
		when(kakaoLocalClient.searchKeyword(new KakaoPlaceSearchRequest(
			"없는장소",
			35.1686,
			129.0576,
			300,
			1,
			5)))
			.thenReturn(new KakaoPlaceSearchResult(List.of(), 0, true));
		when(kakaoLocalClient.reverseGeocode(35.1686, 129.0576))
			.thenReturn(Optional.of(new KakaoAddressDocument(
				"부산 부산진구 범전동 200",
				"부산 부산진구 시민공원로 73",
				"부산",
				"부산진구",
				"범전동")));
		when(bookmarkRepository.existsByUser_UserIdAndBookmarkTargetId(eq(userId), anyString())).thenReturn(false);

		PlaceClickDetailResponse response = placeService.getPlaceDetail(userId, request);

		assertThat(response.detailType()).isEqualTo(PlaceDetailType.EXTERNAL_ADDRESS);
		assertThat(response.providerPlaceId()).isNull();
		assertThat(response.name()).isEqualTo("부산 부산진구 시민공원로 73");
		assertThat(response.address()).isEqualTo("부산 부산진구 시민공원로 73");
	}

	@Test
	@DisplayName("외부 상세 조회는 POI 이름 힌트가 없으면 주소 상세로 fallback한다")
	void getPlaceDetailPoiWithoutNameHintFallsBackToAddress() {
		UUID userId = UUID.randomUUID();
		PlaceClickDetailRequest request = new PlaceClickDetailRequest(
			35.1686,
			129.0576,
			PlaceClickType.POI,
			"KAKAO",
			null,
			null);
		when(kakaoLocalClient.reverseGeocode(35.1686, 129.0576))
			.thenReturn(Optional.of(new KakaoAddressDocument(
				"부산 부산진구 범전동 200",
				"부산 부산진구 시민공원로 73",
				"부산",
				"부산진구",
				"범전동")));
		when(bookmarkRepository.existsByUser_UserIdAndBookmarkTargetId(eq(userId), anyString())).thenReturn(false);

		PlaceClickDetailResponse response = placeService.getPlaceDetail(userId, request);

		assertThat(response.detailType()).isEqualTo(PlaceDetailType.EXTERNAL_ADDRESS);
		assertThat(response.name()).isEqualTo("부산 부산진구 시민공원로 73");
	}

	@Test
	@DisplayName("좌표 주소 변환 결과가 없으면 장소 주소 없음 에러를 반환한다")
	void reverseGeocodeNotFound() {
		when(kakaoLocalClient.reverseGeocode(35.1686, 129.0576)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> placeService.reverseGeocode("35.1686", "129.0576"))
			.isInstanceOf(PlaceException.class)
			.extracting("errorCode")
			.isEqualTo(PlaceErrorCode.PLACE_ADDRESS_NOT_FOUND);
	}

	@Test
	@DisplayName("좌표 주소 변환 외부 API 실패는 원인 예외를 유지한다")
	void preserveReverseGeocodeExternalApiFailureCause() {
		RestClientException cause = new RestClientException("timeout");
		when(kakaoLocalClient.reverseGeocode(35.1686, 129.0576)).thenThrow(cause);

		assertThatThrownBy(() -> placeService.reverseGeocode("35.1686", "129.0576"))
			.isInstanceOf(PlaceException.class)
			.hasCause(cause)
			.extracting("errorCode")
			.isEqualTo(PlaceErrorCode.PLACE_REVERSE_GEOCODE_EXTERNAL_API_FAILED);
	}

	@Test
	@DisplayName("좌표 주소 변환 좌표가 범위를 벗어나면 도메인 에러를 반환한다")
	void rejectInvalidReverseGeocodeCoordinateRange() {
		assertThatThrownBy(() -> placeService.reverseGeocode("91", "129.0576"))
			.isInstanceOf(PlaceException.class)
			.extracting("errorCode")
			.isEqualTo(PlaceErrorCode.INVALID_PLACE_REQUEST);

		verify(kakaoLocalClient, never()).reverseGeocode(
			org.mockito.ArgumentMatchers.anyDouble(),
			org.mockito.ArgumentMatchers.anyDouble());
	}

	@Test
	@DisplayName("주변 장소 조회는 현재 사용자 북마크 여부와 필터를 반영한다")
	void getPlaces() {
		UUID userId = UUID.randomUUID();
		Place matchedPlace = place(
			10L,
			"부산시민공원",
			PlaceCategory.TOURIST_SPOT,
			null,
			35.1686,
			129.0576,
			AccessibilityFeatureType.accessibleToilet);
		when(placeRepository.findPlaceMarkerIds(
			35.1686,
			129.0576,
			500,
			Set.of("TOURIST_SPOT"),
			false,
			Set.of("accessibleToilet"),
			false,
			200))
			.thenReturn(List.of(10L));
		when(placeRepository.findAllByPlaceIdIn(List.of(10L))).thenReturn(List.of(matchedPlace));
		when(bookmarkRepository.findBookmarkedPlaceIds(userId, List.of(10L))).thenReturn(Set.of(10L));

		PlaceListResponse response = placeService.getPlaces(
			userId,
			"35.1686",
			"129.0576",
			"500",
			"TOURIST_SPOT",
			"accessibleToilet");

		assertThat(response.places()).hasSize(1);
		assertThat(response.places().get(0).placeId()).isEqualTo(10L);
		assertThat(response.places().get(0).isBookmarked()).isTrue();
	}

	@Test
	@DisplayName("주변 장소 조회는 반경이 없으면 기본 반경과 최대 개수로 조회한다")
	void getPlacesWithDefaultRadiusAndLimit() {
		when(placeRepository.findPlaceMarkerIds(
			35.1686,
			129.0576,
			1000,
			Set.of("__EMPTY_FILTER__"),
			true,
			Set.of("__EMPTY_FILTER__"),
			true,
			200))
			.thenReturn(List.of());

		PlaceListResponse response = placeService.getPlaces(UUID.randomUUID(), "35.1686", "129.0576", null, null, null);

		assertThat(response.places()).isEmpty();
		verify(placeRepository, never()).findAllByPlaceIdIn(anyCollection());
		verify(bookmarkRepository, never()).findBookmarkedPlaceIds(org.mockito.ArgumentMatchers.any(), anyCollection());
	}

	@Test
	@DisplayName("시설 상세 조회는 장소와 현재 사용자 북마크 여부를 반환한다")
	void getPlace() {
		UUID userId = UUID.randomUUID();
		Place place = place(
			10L,
			"부산시민공원",
			PlaceCategory.TOURIST_SPOT,
			"123456789",
			35.1686,
			129.0576,
			AccessibilityFeatureType.accessibleEntrance);
		when(placeRepository.findWithAccessibilityFeaturesByPlaceId(10L)).thenReturn(Optional.of(place));
		when(bookmarkRepository.existsByUser_UserIdAndPlace_PlaceId(userId, 10L)).thenReturn(true);

		PlaceDetailResponse response = placeService.getPlace(userId, "10");

		assertThat(response.placeId()).isEqualTo(10L);
		assertThat(response.providerPlaceId()).isEqualTo("123456789");
		assertThat(response.isBookmarked()).isTrue();
	}

	@Test
	@DisplayName("검색어가 없으면 장소 검색 도메인 에러를 반환한다")
	void rejectBlankKeyword() {
		assertThatThrownBy(() -> placeService.searchPlaces(" ", null, null, null, null, null))
			.isInstanceOf(PlaceException.class)
			.extracting("errorCode")
			.isEqualTo(PlaceErrorCode.PLACE_KEYWORD_REQUIRED);

		verify(kakaoLocalClient, never()).searchKeyword(org.mockito.ArgumentMatchers.any());
	}

	@Test
	@DisplayName("카카오 API 실패는 원인 예외를 유지한다")
	void preserveExternalApiFailureCause() {
		RestClientException cause = new RestClientException("timeout");
		when(kakaoLocalClient.searchKeyword(new KakaoPlaceSearchRequest(
			"부산시민공원",
			null,
			null,
			null,
			1,
			10)))
			.thenThrow(cause);

		assertThatThrownBy(() -> placeService.searchPlaces("부산시민공원", null, null, null, null, null))
			.isInstanceOf(PlaceException.class)
			.hasCause(cause)
			.extracting("errorCode")
			.isEqualTo(PlaceErrorCode.PLACE_SEARCH_EXTERNAL_API_FAILED);
	}

	@Test
	@DisplayName("주변 장소 조회 반경이 최대 반경을 넘으면 도메인 에러를 반환한다")
	void rejectTooLargePlaceRadius() {
		assertThatThrownBy(() -> placeService.getPlaces(UUID.randomUUID(), "35.1686", "129.0576", "3001", null, null))
			.isInstanceOf(PlaceException.class)
			.extracting("errorCode")
			.isEqualTo(PlaceErrorCode.INVALID_PLACE_REQUEST);

		verify(placeRepository, never()).findPlaceMarkerIds(
			org.mockito.ArgumentMatchers.anyDouble(),
			org.mockito.ArgumentMatchers.anyDouble(),
			org.mockito.ArgumentMatchers.anyInt(),
			anyCollection(),
			org.mockito.ArgumentMatchers.anyBoolean(),
			anyCollection(),
			org.mockito.ArgumentMatchers.anyBoolean(),
			org.mockito.ArgumentMatchers.anyInt());
	}

	@Test
	@DisplayName("잘못된 placeId는 장소 조회 도메인 에러를 반환한다")
	void rejectInvalidPlaceId() {
		assertThatThrownBy(() -> placeService.getPlace(UUID.randomUUID(), "abc"))
			.isInstanceOf(PlaceException.class)
			.extracting("errorCode")
			.isEqualTo(PlaceErrorCode.INVALID_PLACE_REQUEST);

		verify(placeRepository, never()).findWithAccessibilityFeaturesByPlaceId(org.mockito.ArgumentMatchers.any());
	}

	@Test
	@DisplayName("주변 장소 조회 결과가 없으면 북마크 조회를 생략한다")
	void skipBookmarkLookupWhenEmpty() {
		when(placeRepository.findPlaceMarkerIds(
			35.1686,
			129.0576,
			1000,
			Set.of("__EMPTY_FILTER__"),
			true,
			Set.of("__EMPTY_FILTER__"),
			true,
			200))
			.thenReturn(List.of());

		PlaceListResponse response = placeService.getPlaces(UUID.randomUUID(), "35.1686", "129.0576", null, null, null);

		assertThat(response.places()).isEmpty();
		verify(bookmarkRepository, never()).findBookmarkedPlaceIds(org.mockito.ArgumentMatchers.any(), anyCollection());
	}

	private Place place(
		Long placeId,
		String name,
		PlaceCategory category,
		String providerPlaceId,
		double lat,
		double lng,
		AccessibilityFeatureType featureType) {
		Place place = instantiate(Place.class);
		ReflectionTestUtils.setField(place, "placeId", placeId);
		ReflectionTestUtils.setField(place, "name", name);
		ReflectionTestUtils.setField(place, "category", category);
		ReflectionTestUtils.setField(place, "address", "부산광역시");
		ReflectionTestUtils.setField(place, "point", geoPointConverter.toPoint(new GeoPointRequest(lat, lng)));
		ReflectionTestUtils.setField(place, "providerPlaceId", providerPlaceId);
		ReflectionTestUtils.setField(place, "accessibilityFeatures", List.of(feature(place, featureType)));
		return place;
	}

	private PlaceAccessibilityFeature feature(Place place, AccessibilityFeatureType featureType) {
		PlaceAccessibilityFeature feature = instantiate(PlaceAccessibilityFeature.class);
		ReflectionTestUtils.setField(feature, "id", 1);
		ReflectionTestUtils.setField(feature, "place", place);
		ReflectionTestUtils.setField(feature, "featureType", featureType);
		ReflectionTestUtils.setField(feature, "isAvailable", true);
		return feature;
	}

	private <T> T instantiate(Class<T> type) {
		try {
			Constructor<T> constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
