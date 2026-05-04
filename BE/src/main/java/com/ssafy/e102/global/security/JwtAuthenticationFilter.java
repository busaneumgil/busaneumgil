package com.ssafy.e102.global.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final com.ssafy.e102.domain.auth.token.AccessTokenBlacklistStore accessTokenBlacklistStore;

	public JwtAuthenticationFilter(
		JwtTokenProvider jwtTokenProvider,
		com.ssafy.e102.domain.auth.token.AccessTokenBlacklistStore accessTokenBlacklistStore) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.accessTokenBlacklistStore = accessTokenBlacklistStore;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
			String accessToken = bearerToken.substring(BEARER_PREFIX.length());
			if (!accessTokenBlacklistStore.contains(accessToken)) {
				authenticate(request, accessToken);
			}
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(HttpServletRequest request, String token) {
		try {
			AuthPrincipal principal = new AuthPrincipal(jwtTokenProvider.getAccessTokenSubject(token));
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal,
				null, null);
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (JwtTokenException exception) {
			SecurityContextHolder.clearContext();
		}
	}
}
