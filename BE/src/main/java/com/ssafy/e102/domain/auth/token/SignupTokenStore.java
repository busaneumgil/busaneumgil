package com.ssafy.e102.domain.auth.token;

import java.time.Duration;
import java.util.Optional;

public interface SignupTokenStore {

	void save(String signupToken, SignupTokenData signupTokenData, Duration ttl);

	Optional<SignupTokenData> find(String signupToken);

	void delete(String signupToken);
}
