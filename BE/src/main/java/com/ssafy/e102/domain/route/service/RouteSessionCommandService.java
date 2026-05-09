package com.ssafy.e102.domain.route.service;

import java.util.UUID;

import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.route.type.RouteSessionStatus;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.repository.UserRepository;

@Service
public class RouteSessionCommandService {

	private final RouteSessionRepository routeSessionRepository;
	private final UserRepository userRepository;

	public RouteSessionCommandService(RouteSessionRepository routeSessionRepository, UserRepository userRepository) {
		this.routeSessionRepository = routeSessionRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public void saveActiveSessionIfAbsent(
		UUID userId,
		String routeId,
		Point startPoint,
		Point endPoint,
		JsonNode routeSnapshotJson) {
		if (hasActiveSession(userId, routeId)) {
			return;
		}
		User user = userRepository.getReferenceById(userId);
		routeSessionRepository.saveAndFlush(RouteSession.create(
			user,
			routeId,
			startPoint,
			endPoint,
			routeSnapshotJson));
	}

	@Transactional(readOnly = true)
	public boolean hasActiveSession(UUID userId, String routeId) {
		return routeSessionRepository
			.findFirstByUser_UserIdAndRouteIdAndStatusOrderByUpdatedAtDesc(userId, routeId, RouteSessionStatus.ACTIVE)
			.isPresent();
	}
}
