package com.ssafy.e102.domain.route.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e102.domain.route.entity.RouteSession;

public interface RouteSessionRepository extends JpaRepository<RouteSession, UUID> {

	Optional<RouteSession> findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(UUID userId, String routeId);

	Optional<RouteSession> findFirstByRouteIdOrderByUpdatedAtDesc(String routeId);
}
