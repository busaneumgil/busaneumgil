package com.ssafy.e102.domain.bookmark.service;

import java.util.UUID;

import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.bookmark.dto.request.CreateFavoriteRouteRequest;
import com.ssafy.e102.domain.bookmark.dto.request.UpdateFavoriteRouteRequest;
import com.ssafy.e102.domain.bookmark.dto.response.FavoriteRouteIdResponse;
import com.ssafy.e102.domain.bookmark.dto.response.FavoriteRouteListResponse;
import com.ssafy.e102.domain.bookmark.entity.FavoriteRoute;
import com.ssafy.e102.domain.bookmark.exception.FavoriteRouteErrorCode;
import com.ssafy.e102.domain.bookmark.exception.FavoriteRouteException;
import com.ssafy.e102.domain.bookmark.repository.FavoriteRouteRepository;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.exception.UserErrorCode;
import com.ssafy.e102.domain.user.exception.UserException;
import com.ssafy.e102.domain.user.repository.UserRepository;
import com.ssafy.e102.global.geo.GeoPointConverter;

@Service
@Transactional(readOnly = true)
public class FavoriteRouteService {

	private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "favRouteId");

	private final FavoriteRouteRepository favoriteRouteRepository;
	private final UserRepository userRepository;
	private final GeoPointConverter geoPointConverter;

	public FavoriteRouteService(
		FavoriteRouteRepository favoriteRouteRepository,
		UserRepository userRepository,
		GeoPointConverter geoPointConverter) {
		this.favoriteRouteRepository = favoriteRouteRepository;
		this.userRepository = userRepository;
		this.geoPointConverter = geoPointConverter;
	}

	public FavoriteRouteListResponse getFavoriteRoutes(UUID userId, Long cursor, int size) {
		PageRequest pageRequest = PageRequest.of(0, size, NEWEST_FIRST);
		Slice<FavoriteRoute> favoriteRoutes = cursor == null
			? favoriteRouteRepository.findAllByUser_UserId(userId, pageRequest)
			: favoriteRouteRepository.findAllByUser_UserIdAndFavRouteIdLessThan(userId, cursor, pageRequest);
		return FavoriteRouteListResponse.of(
			favoriteRoutes.getContent(),
			size,
			favoriteRoutes.hasNext(),
			geoPointConverter);
	}

	@Transactional
	public FavoriteRouteIdResponse createFavoriteRoute(UUID userId, CreateFavoriteRouteRequest request) {
		User user = getUser(userId);
		FavoriteRoute favoriteRoute = FavoriteRoute.create(
			user,
			request.startLabel(),
			request.endLabel(),
			geoPointConverter.toPoint(request.startPoint()),
			geoPointConverter.toPoint(request.endPoint()),
			request.routeOption());
		FavoriteRoute savedFavoriteRoute = favoriteRouteRepository.save(favoriteRoute);
		return new FavoriteRouteIdResponse(savedFavoriteRoute.getFavRouteId());
	}

	@Transactional
	public FavoriteRouteIdResponse updateFavoriteRoute(
		UUID userId,
		Long favRouteId,
		UpdateFavoriteRouteRequest request) {
		if (!request.hasAnyField()) {
			throw new FavoriteRouteException(
				FavoriteRouteErrorCode.INVALID_FAVORITE_ROUTE_UPDATE_REQUEST,
				"수정할 경로 북마크 필드가 필요합니다.");
		}

		FavoriteRoute favoriteRoute = getFavoriteRoute(favRouteId);
		validateOwner(favoriteRoute, userId);
		favoriteRoute.changeRoute(
			valueOrDefault(request.startLabel(), favoriteRoute.getStartLabel()),
			valueOrDefault(request.endLabel(), favoriteRoute.getEndLabel()),
			pointOrDefault(request.startPoint(), favoriteRoute.getStartPoint()),
			pointOrDefault(request.endPoint(), favoriteRoute.getEndPoint()),
			valueOrDefault(request.routeOption(), favoriteRoute.getRouteOption()));
		return new FavoriteRouteIdResponse(favoriteRoute.getFavRouteId());
	}

	@Transactional
	public void deleteFavoriteRoute(UUID userId, Long favRouteId) {
		FavoriteRoute favoriteRoute = getFavoriteRoute(favRouteId);
		validateOwner(favoriteRoute, userId);
		favoriteRouteRepository.delete(favoriteRoute);
	}

	private User getUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
	}

	private FavoriteRoute getFavoriteRoute(Long favRouteId) {
		return favoriteRouteRepository.findById(favRouteId)
			.orElseThrow(() -> new FavoriteRouteException(FavoriteRouteErrorCode.FAVORITE_ROUTE_NOT_FOUND));
	}

	private void validateOwner(FavoriteRoute favoriteRoute, UUID userId) {
		if (!favoriteRoute.isOwner(userId)) {
			throw new FavoriteRouteException(FavoriteRouteErrorCode.FAVORITE_ROUTE_FORBIDDEN);
		}
	}

	private Point pointOrDefault(com.ssafy.e102.global.geo.dto.GeoPointRequest point, Point defaultValue) {
		if (point == null) {
			return defaultValue;
		}
		return geoPointConverter.toPoint(point);
	}

	private static <T> T valueOrDefault(T value, T defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		return value;
	}
}
