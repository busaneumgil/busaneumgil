package com.ssafy.e102.domain.place.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ssafy.e102.domain.place.dto.response.PlaceDetailResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceListResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceMarkerResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceSearchResponse;
import com.ssafy.e102.domain.place.service.PlaceService;
import com.ssafy.e102.domain.place.type.PlaceCategory;
import com.ssafy.e102.global.geo.dto.GeoPointResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

class PlaceControllerTest {

	@Mock
	private PlaceService placeService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(new PlaceController(placeService))
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.build();
	}

	@Test
	@DisplayName("장소 검색은 query parameter를 서비스에 전달한다")
	void searchPlaces() throws Exception {
		when(placeService.searchPlaces("부산시민공원", "35.1686", "129.0576", "1000", "0", "10"))
			.thenReturn(new PlaceSearchResponse(List.of(), 0, 10, 0, 0, false));

		mockMvc.perform(get("/places/search")
			.param("keyword", "부산시민공원")
			.param("lat", "35.1686")
			.param("lng", "129.0576")
			.param("radius", "1000")
			.param("page", "0")
			.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("S2000"))
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.hasNext").value(false));
	}

	@Test
	@DisplayName("주변 시설 조회는 현재 사용자와 필터를 서비스에 전달한다")
	void getPlaces() throws Exception {
		UUID userId = UUID.randomUUID();
		UsernamePasswordAuthenticationToken authentication = authentication(userId);
		when(placeService.getPlaces(
			userId,
			"35.1686",
			"129.0576",
			"500",
			"TOURIST_SPOT",
			"accessibleToilet"))
			.thenReturn(new PlaceListResponse(List.of(new PlaceMarkerResponse(
				10L,
				"부산시민공원",
				PlaceCategory.TOURIST_SPOT,
				"부산광역시",
				new GeoPointResponse(35.1686, 129.0576),
				List.of(),
				true))));

		mockMvc.perform(get("/places")
			.principal(authentication)
			.param("lat", "35.1686")
			.param("lng", "129.0576")
			.param("radius", "500")
			.param("category", "TOURIST_SPOT")
			.param("featureType", "accessibleToilet"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.places[0].placeId").value(10))
			.andExpect(jsonPath("$.data.places[0].isBookmarked").value(true));

		verify(placeService).getPlaces(userId, "35.1686", "129.0576", "500", "TOURIST_SPOT", "accessibleToilet");
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("시설 상세 조회는 현재 사용자와 placeId를 서비스에 전달한다")
	void getPlace() throws Exception {
		UUID userId = UUID.randomUUID();
		UsernamePasswordAuthenticationToken authentication = authentication(userId);
		when(placeService.getPlace(userId, "10"))
			.thenReturn(new PlaceDetailResponse(
				10L,
				"부산시민공원",
				PlaceCategory.TOURIST_SPOT,
				"부산광역시",
				new GeoPointResponse(35.1686, 129.0576),
				"123456789",
				List.of(),
				false));

		mockMvc.perform(get("/places/10")
			.principal(authentication))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.placeId").value(10))
			.andExpect(jsonPath("$.data.isBookmarked").value(false));

		verify(placeService).getPlace(userId, "10");
		SecurityContextHolder.clearContext();
	}

	private UsernamePasswordAuthenticationToken authentication(UUID userId) {
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			new AuthPrincipal(userId, "access-token"), null);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		return authentication;
	}
}
