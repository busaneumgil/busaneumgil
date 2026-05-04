package com.ssafy.e102.domain.auth.token;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RedisSignupTokenStore implements SignupTokenStore {

	private static final String KEY_PREFIX = "auth:signup:";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public RedisSignupTokenStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void save(String signupToken, SignupTokenData signupTokenData, Duration ttl) {
		redisTemplate.opsForValue().set(key(signupToken), serialize(signupTokenData), ttl.toSeconds(),
			TimeUnit.SECONDS);
	}

	@Override
	public Optional<SignupTokenData> find(String signupToken) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(signupToken)))
			.map(this::deserialize);
	}

	@Override
	public void delete(String signupToken) {
		redisTemplate.delete(key(signupToken));
	}

	private String key(String token) {
		return KEY_PREFIX + TokenHash.sha256(token);
	}

	private String serialize(SignupTokenData signupTokenData) {
		try {
			return objectMapper.writeValueAsString(signupTokenData);
		} catch (JsonProcessingException exception) {
			throw new TokenStoreException("회원가입 토큰 정보를 직렬화할 수 없습니다.", exception);
		}
	}

	private SignupTokenData deserialize(String value) {
		try {
			return objectMapper.readValue(value, SignupTokenData.class);
		} catch (JsonProcessingException exception) {
			throw new TokenStoreException("회원가입 토큰 정보를 역직렬화할 수 없습니다.", exception);
		}
	}
}
