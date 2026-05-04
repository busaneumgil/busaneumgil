package com.ssafy.e102.domain.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.user.service.UserService;
import com.ssafy.e102.global.security.AuthPrincipal;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> withdraw(
		Authentication authentication,
		@RequestHeader(HttpHeaders.AUTHORIZATION)
		String authorizationHeader) {
		AuthPrincipal principal = (AuthPrincipal)authentication.getPrincipal();
		userService.withdraw(principal.userId(), extractBearerToken(authorizationHeader));
		return ResponseEntity.noContent().build();
	}

	private String extractBearerToken(String authorizationHeader) {
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
