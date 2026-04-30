package com.ssafy.e102.domain.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.user.type.SocialProvider;

class RedisSignupTokenStoreTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private RedisSignupTokenStore signupTokenStore;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		signupTokenStore = new RedisSignupTokenStore(redisTemplate, new ObjectMapper());
	}

	@Test
	@DisplayName("회원가입 토큰은 소셜 식별 정보를 저장한다")
	void saveSignupToken() {
		Duration ttl = Duration.ofMinutes(10);

		signupTokenStore.save("signup-token", new SignupTokenData(SocialProvider.KAKAO, "kakao-user-id"), ttl);

		verify(valueOperations).set(
			"auth:signup:932739eece2b7d31922b6d13a4a5f9caa895139a7d8bc549472a5682b624f9b5",
			"{\"socialProvider\":\"KAKAO\",\"socialProviderUserId\":\"kakao-user-id\"}",
			ttl.toSeconds(),
			TimeUnit.SECONDS);
	}

	@Test
	@DisplayName("저장된 회원가입 토큰으로 소셜 식별 정보를 조회한다")
	void findSignupTokenData() {
		when(valueOperations.get("auth:signup:932739eece2b7d31922b6d13a4a5f9caa895139a7d8bc549472a5682b624f9b5"))
			.thenReturn("{\"socialProvider\":\"NAVER\",\"socialProviderUserId\":\"naver-user-id\"}");

		Optional<SignupTokenData> found = signupTokenStore.find("signup-token");

		assertThat(found).contains(new SignupTokenData(SocialProvider.NAVER, "naver-user-id"));
	}

	@Test
	@DisplayName("회원가입 완료 후 임시 토큰을 삭제한다")
	void deleteSignupToken() {
		signupTokenStore.delete("signup-token");

		verify(redisTemplate).delete("auth:signup:932739eece2b7d31922b6d13a4a5f9caa895139a7d8bc549472a5682b624f9b5");
	}
}
