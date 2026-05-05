package com.ssafy.e102.domain.route.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ssafy.e102.domain.route.entity.FavoriteRoute;
import com.ssafy.e102.global.geo.GeoPointConverter;

public record FavoriteRouteListResponse(
	List<FavoriteRouteResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext) {

	public static FavoriteRouteListResponse from(
		Page<FavoriteRoute> favoriteRoutes,
		GeoPointConverter geoPointConverter) {
		return new FavoriteRouteListResponse(
			favoriteRoutes.getContent()
				.stream()
				.map(favoriteRoute -> FavoriteRouteResponse.from(favoriteRoute, geoPointConverter))
				.toList(),
			favoriteRoutes.getNumber(),
			favoriteRoutes.getSize(),
			favoriteRoutes.getTotalElements(),
			favoriteRoutes.getTotalPages(),
			favoriteRoutes.hasNext());
	}
}
