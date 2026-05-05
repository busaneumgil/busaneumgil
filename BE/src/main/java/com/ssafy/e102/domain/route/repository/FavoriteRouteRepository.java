package com.ssafy.e102.domain.route.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e102.domain.route.entity.FavoriteRoute;

public interface FavoriteRouteRepository extends JpaRepository<FavoriteRoute, Long> {

	Page<FavoriteRoute> findAllByUser_UserId(UUID userId, Pageable pageable);
}
