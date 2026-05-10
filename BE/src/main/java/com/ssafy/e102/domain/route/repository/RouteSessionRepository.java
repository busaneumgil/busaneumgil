package com.ssafy.e102.domain.route.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.type.RouteSessionStatus;

public interface RouteSessionRepository extends JpaRepository<RouteSession, UUID> {

	Optional<RouteSession> findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(UUID userId, String routeId);

	Optional<RouteSession> findFirstByUser_UserIdAndRouteIdAndStatusOrderByUpdatedAtDesc(
		UUID userId,
		String routeId,
		RouteSessionStatus status);

	List<RouteSession> findAllByUser_UserIdAndRouteIdAndStatusOrderByUpdatedAtDesc(
		UUID userId,
		String routeId,
		RouteSessionStatus status);

	Optional<RouteSession> findFirstByRouteIdOrderByUpdatedAtDesc(String routeId);

	void deleteAllByUser_UserId(UUID userId);
}
