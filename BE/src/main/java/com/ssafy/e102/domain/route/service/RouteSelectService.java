package com.ssafy.e102.domain.route.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ssafy.e102.domain.route.dto.request.SelectRouteRequest;

/**
 * 사용자가 검색 후보 중 하나를 실제 안내 route session으로 확정한다.
 */
@Service
public class RouteSelectService {

	public void select(UUID userId, String routeId, SelectRouteRequest request) {
		// Route cache ownership and session persistence are implemented in following commits.
	}
}
