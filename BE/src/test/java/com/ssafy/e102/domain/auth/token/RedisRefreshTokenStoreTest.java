package com.ssafy.e102.domain.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisRefreshTokenStoreTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private RedisRefreshTokenStore refreshTokenStore;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		refreshTokenStore = new RedisRefreshTokenStore(redisTemplate);
	}

	@Test
	@DisplayName("리프레시 토큰은 원문 대신 해시 키로 저장한다")
	void saveRefreshTokenWithHashedKey() {
		UUID userId = UUID.randomUUID();
		Duration ttl = Duration.ofDays(14);

		refreshTokenStore.save("refresh-token", userId, ttl);

		verify(valueOperations).set(
			"auth:refresh:0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120",
			userId.toString(),
			ttl.toSeconds(),
			TimeUnit.SECONDS);
	}

	@Test
	@DisplayName("저장된 리프레시 토큰으로 사용자 ID를 조회한다")
	void findUserIdByRefreshToken() {
		UUID userId = UUID.randomUUID();
		when(valueOperations.get("auth:refresh:0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120"))
			.thenReturn(userId.toString());

		Optional<UUID> found = refreshTokenStore.findUserId("refresh-token");

		assertThat(found).contains(userId);
	}

	@Test
	@DisplayName("리프레시 토큰 회전은 기존 토큰을 지우고 새 토큰을 저장한다")
	void rotateRefreshToken() {
		UUID userId = UUID.randomUUID();
		Duration ttl = Duration.ofDays(14);

		refreshTokenStore.rotate("old-refresh-token", "new-refresh-token", userId, ttl);

		verify(redisTemplate).delete("auth:refresh:66e4f4e9739a9ef9a9d6e414cfd05780c4ab0eb03e21fbf90ebf87e76d4db8f6");
		verify(valueOperations).set(
			"auth:refresh:c40dd1765d767caae2588f0ee1de9181d8a44cc9306261eb2c9e526351188338",
			userId.toString(),
			ttl.toSeconds(),
			TimeUnit.SECONDS);
	}
}
