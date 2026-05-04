package com.ssafy.e102.domain.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class TokenHash {

	static String sha256(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(token.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(digest.length * 2);
			for (byte value : digest) {
				builder.append(String.format("%02x", value));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new TokenStoreException("토큰 해시 알고리즘을 사용할 수 없습니다.", exception);
		}
	}
}
