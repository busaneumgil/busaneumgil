package com.ssafy.e102.global.security.filter;

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

import com.ssafy.e102.domain.auth.token.AuthTokenStore;
import com.ssafy.e102.global.security.jwt.JwtTokenException;
import com.ssafy.e102.global.security.jwt.JwtTokenProvider;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final AuthTokenStore authTokenStore;

	public JwtAuthenticationFilter(
		JwtTokenProvider jwtTokenProvider,
		AuthTokenStore authTokenStore) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.authTokenStore = authTokenStore;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
			String accessToken = bearerToken.substring(BEARER_PREFIX.length());
			if (!authTokenStore.containsAccessToken(accessToken)) {
				authenticate(request, accessToken);
			}
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(HttpServletRequest request, String token) {
		try {
			AuthPrincipal principal = new AuthPrincipal(jwtTokenProvider.getAccessTokenSubject(token), token);
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal,
				null, null);
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (JwtTokenException exception) {
			SecurityContextHolder.clearContext();
		}
	}
}
