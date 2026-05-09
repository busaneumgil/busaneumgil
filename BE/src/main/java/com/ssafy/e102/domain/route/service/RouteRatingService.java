package com.ssafy.e102.domain.route.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.e102.domain.route.dto.request.RouteRatingRequest;
import com.ssafy.e102.domain.route.dto.response.RouteRatingResponse;
import com.ssafy.e102.domain.route.entity.RouteRating;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.RouteRatingRepository;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class RouteRatingService {

	private final RouteRatingRepository routeRatingRepository;
	private final RouteSessionRepository routeSessionRepository;
	private final UserRepository userRepository;

	public RouteRatingService(
		RouteRatingRepository routeRatingRepository,
		RouteSessionRepository routeSessionRepository,
		UserRepository userRepository) {
		this.routeRatingRepository = routeRatingRepository;
		this.routeSessionRepository = routeSessionRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public RouteRatingResponse rate(UUID userId, RouteRatingRequest request) {
		Optional<RouteSession> routeSession = routeSessionRepository
			.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(userId, request.routeId());
		JsonNode routeContextJson = routeSession
			.map(RouteSession::getRouteSnapshotJson)
			.orElseGet(() -> routeContextOrThrowIfOwnedByOtherUser(request.routeId()));

		RouteRating routeRating = routeRatingRepository.findByUser_UserIdAndRouteId(userId, request.routeId())
			.map(existingRating -> {
				existingRating.updateScore(request.score(), routeContextJson);
				return existingRating;
			})
			.orElseGet(() -> createRating(userId, request, routeContextJson));
		return new RouteRatingResponse(routeRating.getRatingId());
	}

	private JsonNode routeContextOrThrowIfOwnedByOtherUser(String routeId) {
		if (routeSessionRepository.findFirstByRouteIdOrderByUpdatedAtDesc(routeId).isPresent()) {
			throw new RouteException(RouteErrorCode.ROUTE_ACCESS_DENIED);
		}
		return null;
	}

	private RouteRating createRating(UUID userId, RouteRatingRequest request, JsonNode routeContextJson) {
		User user = userRepository.getReferenceById(userId);
		return routeRatingRepository.save(RouteRating.create(
			user,
			request.routeId(),
			request.score(),
			routeContextJson));
	}
}
