package com.ssafy.e102.domain.auth.token;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {

	private static final String KEY_PREFIX = "auth:refresh:";

	private final StringRedisTemplate redisTemplate;

	public RedisRefreshTokenStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void save(String refreshToken, UUID userId, Duration ttl) {
		redisTemplate.opsForValue().set(key(refreshToken), userId.toString(), ttl.toSeconds(), TimeUnit.SECONDS);
	}

	@Override
	public Optional<UUID> findUserId(String refreshToken) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(refreshToken)))
			.map(UUID::fromString);
	}

	@Override
	public void rotate(String oldRefreshToken, String newRefreshToken, UUID userId, Duration ttl) {
		delete(oldRefreshToken);
		save(newRefreshToken, userId, ttl);
	}

	@Override
	public void delete(String refreshToken) {
		redisTemplate.delete(key(refreshToken));
	}

	private String key(String token) {
		return KEY_PREFIX + TokenHash.sha256(token);
	}
}
