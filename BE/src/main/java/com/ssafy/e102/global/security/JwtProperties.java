package com.ssafy.e102.global.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
	String secret,
	String issuer,
	Duration accessTokenTtl,
	Duration refreshTokenTtl,
	Duration signupTokenTtl) {

	public JwtProperties {
		Assert.hasText(secret, "jwt.secret must not be blank");
		Assert.hasText(issuer, "jwt.issuer must not be blank");
		Assert.notNull(accessTokenTtl, "jwt.access-token-ttl must not be null");
		Assert.notNull(refreshTokenTtl, "jwt.refresh-token-ttl must not be null");
		Assert.notNull(signupTokenTtl, "jwt.signup-token-ttl must not be null");
		Assert.isTrue(!accessTokenTtl.isNegative() && !accessTokenTtl.isZero(),
			"jwt.access-token-ttl must be positive");
		Assert.isTrue(!refreshTokenTtl.isNegative() && !refreshTokenTtl.isZero(),
			"jwt.refresh-token-ttl must be positive");
		Assert.isTrue(!signupTokenTtl.isNegative() && !signupTokenTtl.isZero(),
			"jwt.signup-token-ttl must be positive");
	}
}
