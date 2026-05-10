package com.ssafy.e102.global.security.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ssafy.e102.E102Application;
import com.ssafy.e102.domain.auth.dto.response.SocialLoginResponse;
import com.ssafy.e102.domain.auth.dto.response.TokenResponse;
import com.ssafy.e102.domain.auth.service.AuthService;
import com.ssafy.e102.domain.auth.token.AuthTokenStore;
import com.ssafy.e102.domain.bookmark.service.FavoriteRouteService;
import com.ssafy.e102.domain.report.service.HazardReportService;
import com.ssafy.e102.domain.route.service.RouteRatingService;
import com.ssafy.e102.domain.route.service.WalkRouteSearchService;
import com.ssafy.e102.domain.user.dto.response.UserMeResponse;
import com.ssafy.e102.domain.user.repository.UserRepository;
import com.ssafy.e102.domain.user.service.UserService;
import com.ssafy.e102.domain.user.type.PrimaryUserType;
import com.ssafy.e102.domain.user.type.SocialProvider;
import com.ssafy.e102.domain.user.type.UserRole;
import com.ssafy.e102.global.security.jwt.JwtTokenProvider;

@SpringBootTest(classes = E102Application.class)
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private AuthTokenStore authTokenStore;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private FavoriteRouteService favoriteRouteService;

	@MockitoBean
	private HazardReportService hazardReportService;

	@MockitoBean
	private WalkRouteSearchService walkRouteSearchService;

	@MockitoBean
	private RouteRatingService routeRatingService;

	@Test
	@DisplayName("소셜 로그인은 인증 없이 접근할 수 있다")
	void socialLoginIsPublic() throws Exception {
		when(authService.socialLogin(any()))
			.thenReturn(SocialLoginResponse.existingUser(
				"access-token",
				"refresh-token",
				UUID.randomUUID(),
				PrimaryUserType.LOW_VISION,
				null));

		mockMvc.perform(post("/auth/social-login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"socialProvider\":\"KAKAO\",\"socialAccessToken\":\"kakao-access-token\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("S2000"));
	}

	@Test
	@DisplayName("토큰 재발급은 인증 없이 접근할 수 있다")
	void reissueIsPublic() throws Exception {
		when(authService.reissue(any())).thenReturn(new TokenResponse("new-access-token", "new-refresh-token"));

		mockMvc.perform(post("/auth/reissue")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"refresh-token\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("S2000"));
	}

	@Test
	@DisplayName("내 정보 조회는 인증이 필요하다")
	void usersMeRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value("A4010"))
			.andExpect(jsonPath("$.message").value("인증이 필요합니다."));
	}

	@Test
	@DisplayName("로그아웃은 인증이 필요하다")
	void logoutRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/auth/logout"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value("A4010"))
			.andExpect(jsonPath("$.message").value("인증이 필요합니다."));
	}

	@Test
	@DisplayName("경로 북마크 API는 인증이 필요하다")
	void favoriteRoutesRequireAuthentication() throws Exception {
		mockMvc.perform(get("/favorite-routes"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value("A4010"))
			.andExpect(jsonPath("$.message").value("인증이 필요합니다."));
	}

	@Test
	@DisplayName("도보 경로 검색 API는 인증이 필요하다")
	void walkRouteSearchRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/routes/search/walk")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "startPoint": {"lat": 35.12, "lng": 128.936},
				  "endPoint": {"lat": 35.1315, "lng": 128.8823}
				}
				"""))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value("A4010"))
			.andExpect(jsonPath("$.message").value("인증이 필요합니다."));
	}

	@Test
	@DisplayName("경로 평가 API는 인증이 필요하다")
	void routeRatingsRequireAuthentication() throws Exception {
		mockMvc.perform(post("/route-ratings")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "sessionId": "00000000-0000-0000-0000-000000000099",
				  "score": 5
				}
				"""))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value("A4010"))
			.andExpect(jsonPath("$.message").value("인증이 필요합니다."));
	}

	@Test
	@DisplayName("도로 상태 제보 API는 인증이 필요하다")
	void hazardReportsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/hazard-reports/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value("A4010"))
			.andExpect(jsonPath("$.message").value("인증이 필요합니다."));
	}

	@Test
	@DisplayName("유효한 액세스 토큰은 인증 주체를 만든다")
	void validAccessTokenCreatesPrincipal() throws Exception {
		UUID userId = UUID.randomUUID();
		String accessToken = jwtTokenProvider.createAccessToken(userId);
		when(userRepository.existsByUserIdAndRole(userId, UserRole.ADMIN)).thenReturn(false);
		when(userService.getMe(userId))
			.thenReturn(new UserMeResponse(userId, SocialProvider.KAKAO, PrimaryUserType.LOW_VISION, null));

		mockMvc.perform(get("/users/me")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.userId").value(userId.toString()));
	}

	@Test
	@DisplayName("블랙리스트에 있는 액세스 토큰은 인증하지 않는다")
	void blacklistedAccessTokenIsRejected() throws Exception {
		UUID userId = UUID.randomUUID();
		String accessToken = jwtTokenProvider.createAccessToken(userId);
		when(authTokenStore.containsAccessToken(accessToken)).thenReturn(true);

		mockMvc.perform(get("/users/me")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value("A4010"));
	}
}
