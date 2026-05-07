package com.ssafy.e102.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
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

import com.ssafy.e102.domain.place.dto.response.PlaceDetailResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceListResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceSearchResponse;
import com.ssafy.e102.domain.place.entity.Place;
import com.ssafy.e102.domain.place.entity.PlaceAccessibilityFeature;
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
		Place filteredPlace = place(
			11L,
			"부산역",
			PlaceCategory.PUBLIC_OFFICE,
			null,
			35.1152,
			129.0422,
			AccessibilityFeatureType.elevator);
		when(placeRepository.findPlaceMarkers(
			35.1686,
			129.0576,
			500,
			Set.of("TOURIST_SPOT"),
			false,
			Set.of("accessibleToilet"),
			false))
			.thenReturn(List.of(matchedPlace));
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
		when(placeRepository.findPlaceMarkers(
			35.1686,
			129.0576,
			null,
			Set.of("__EMPTY_FILTER__"),
			true,
			Set.of("__EMPTY_FILTER__"),
			true))
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
