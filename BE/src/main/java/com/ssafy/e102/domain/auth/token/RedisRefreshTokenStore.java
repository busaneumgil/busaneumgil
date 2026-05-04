package com.ssafy.e102.domain.auth.token;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {

	private static final String KEY_PREFIX = "auth:refresh:";
	private static final String USER_KEY_PREFIX = "auth:refresh:user:";

	private final StringRedisTemplate redisTemplate;

	public RedisRefreshTokenStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void save(String refreshToken, UUID userId, Duration ttl) {
		String tokenHash = TokenHash.sha256(refreshToken);
		redisTemplate.opsForValue().set(tokenKey(tokenHash), userId.toString(), ttl.toSeconds(), TimeUnit.SECONDS);
		redisTemplate.opsForSet().add(userKey(userId), tokenHash);
		redisTemplate.expire(userKey(userId), ttl.toSeconds(), TimeUnit.SECONDS);
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
		String tokenHash = TokenHash.sha256(refreshToken);
		String tokenKey = tokenKey(tokenHash);
		Optional.ofNullable(redisTemplate.opsForValue().get(tokenKey))
			.map(UUID::fromString)
			.ifPresent(userId -> redisTemplate.opsForSet().remove(userKey(userId), tokenHash));
		redisTemplate.delete(tokenKey);
	}

	@Override
	public void deleteByUserId(UUID userId) {
		String userKey = userKey(userId);
		Set<String> tokenHashes = redisTemplate.opsForSet().members(userKey);
		if (tokenHashes != null && !tokenHashes.isEmpty()) {
			redisTemplate.delete(tokenHashes.stream()
				.map(this::tokenKey)
				.toList());
		}
		redisTemplate.delete(userKey);
	}

	private String key(String token) {
		return tokenKey(TokenHash.sha256(token));
	}

	private String tokenKey(String tokenHash) {
		return KEY_PREFIX + tokenHash;
	}

	private String userKey(UUID userId) {
		return USER_KEY_PREFIX + userId;
	}
}
