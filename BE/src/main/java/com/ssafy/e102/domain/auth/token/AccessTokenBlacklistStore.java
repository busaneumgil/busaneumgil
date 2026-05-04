package com.ssafy.e102.domain.auth.token;

import java.time.Duration;

public interface AccessTokenBlacklistStore {

	void save(String accessToken, Duration ttl);

	boolean contains(String accessToken);
}
