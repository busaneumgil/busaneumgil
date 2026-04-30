package com.ssafy.e102.global.security;

import java.util.UUID;

public record AuthPrincipal(
	UUID userId) {

	public static AuthPrincipal from(String userId) {
		return new AuthPrincipal(UUID.fromString(userId));
	}
}
