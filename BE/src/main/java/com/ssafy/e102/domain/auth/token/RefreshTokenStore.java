package com.ssafy.e102.domain.auth.token;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {

	void save(String refreshToken, UUID userId, Duration ttl);

	Optional<UUID> findUserId(String refreshToken);

	void rotate(String oldRefreshToken, String newRefreshToken, UUID userId, Duration ttl);

	void delete(String refreshToken);
}
