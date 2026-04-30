package com.ssafy.e102.global.security;

public class JwtTokenException extends RuntimeException {

	public JwtTokenException(String message) {
		super(message);
	}
}
