package com.ssafy.e102.domain.route.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ssafy.e102.domain.route.dto.request.TransitRefreshRequest;
import com.ssafy.e102.domain.route.dto.response.TransitArrivalStatus;
import com.ssafy.e102.domain.route.dto.response.TransitRefreshResponse;
import com.ssafy.e102.domain.route.type.TransportMode;

@Service
public class TransitRefreshService {

	public TransitRefreshResponse refresh(UUID userId, String routeId, TransitRefreshRequest request) {
		return new TransitRefreshResponse(TransportMode.BUS, TransitArrivalStatus.ARRIVAL_UNKNOWN, List.of());
	}
}
