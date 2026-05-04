package com.ssafy.e102.domain.auth.token;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisAccessTokenBlacklistStore implements AccessTokenBlacklistStore {

	private static final String KEY_PREFIX = "auth:access-blacklist:";

	private final StringRedisTemplate redisTemplate;

	public RedisAccessTokenBlacklistStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void save(String accessToken, Duration ttl) {
		if (ttl.isNegative() || ttl.isZero()) {
			return;
		}
		redisTemplate.opsForValue().set(key(accessToken), "1", ttl.toSeconds(), TimeUnit.SECONDS);
	}

	@Override
	public boolean contains(String accessToken) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(key(accessToken)));
	}

	private String key(String accessToken) {
		return KEY_PREFIX + TokenHash.sha256(accessToken);
	}
}
