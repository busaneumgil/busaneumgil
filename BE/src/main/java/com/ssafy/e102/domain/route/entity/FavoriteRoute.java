package com.ssafy.e102.domain.route.entity;

import java.util.Objects;
import java.util.UUID;

import org.locationtech.jts.geom.Point;

import com.ssafy.e102.domain.route.exception.FavoriteRouteErrorCode;
import com.ssafy.e102.domain.route.exception.FavoriteRouteException;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "favorite_routes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteRoute extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, updatable = false)
	private Long favRouteId;

	@Column(nullable = false, length = 511)
	private String routeName;

	@Column(nullable = false, length = 255)
	private String startLabel;

	@Column(nullable = false, length = 255)
	private String endLabel;

	@Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
	private Point startPoint;

	@Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
	private Point endPoint;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private RouteOption routeOption;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "userId", nullable = false)
	private User user;

	public static FavoriteRoute create(
		User user,
		String startLabel,
		String endLabel,
		Point startPoint,
		Point endPoint,
		RouteOption routeOption) {
		FavoriteRoute favoriteRoute = new FavoriteRoute();
		favoriteRoute.user = requireUser(user);
		favoriteRoute.changeRoute(
			startLabel,
			endLabel,
			startPoint,
			endPoint,
			routeOption,
			FavoriteRouteErrorCode.INVALID_FAVORITE_ROUTE_REQUEST);
		return favoriteRoute;
	}

	public void changeRoute(
		String startLabel,
		String endLabel,
		Point startPoint,
		Point endPoint,
		RouteOption routeOption) {
		changeRoute(
			startLabel,
			endLabel,
			startPoint,
			endPoint,
			routeOption,
			FavoriteRouteErrorCode.INVALID_FAVORITE_ROUTE_UPDATE_REQUEST);
	}

	private void changeRoute(
		String startLabel,
		String endLabel,
		Point startPoint,
		Point endPoint,
		RouteOption routeOption,
		FavoriteRouteErrorCode errorCode) {
		String normalizedStartLabel = normalizeLabel(startLabel, "출발지명", errorCode);
		String normalizedEndLabel = normalizeLabel(endLabel, "도착지명", errorCode);
		this.startLabel = normalizedStartLabel;
		this.endLabel = normalizedEndLabel;
		this.startPoint = requirePoint(startPoint, "출발지 좌표", errorCode);
		this.endPoint = requirePoint(endPoint, "도착지 좌표", errorCode);
		this.routeOption = requireRouteOption(routeOption, errorCode);
		this.routeName = generateRouteName(normalizedStartLabel, normalizedEndLabel);
	}

	public boolean isOwner(UUID userId) {
		return user != null && Objects.equals(user.getUserId(), userId);
	}

	private static User requireUser(User user) {
		if (user == null) {
			throw invalidRequest("사용자는 필수입니다.");
		}
		return user;
	}

	private static Point requirePoint(Point point, String fieldName, FavoriteRouteErrorCode errorCode) {
		if (point == null) {
			throw invalidRequest(errorCode, fieldName + "는 필수입니다.");
		}
		return point;
	}

	private static RouteOption requireRouteOption(RouteOption routeOption, FavoriteRouteErrorCode errorCode) {
		if (routeOption == null) {
			throw invalidRequest(errorCode, "경로 옵션은 필수입니다.");
		}
		return routeOption;
	}

	private static String normalizeLabel(String label, String fieldName, FavoriteRouteErrorCode errorCode) {
		if (label == null || label.isBlank()) {
			throw invalidRequest(errorCode, fieldName + "은 필수입니다.");
		}
		return label.trim();
	}

	private static String generateRouteName(String startLabel, String endLabel) {
		return startLabel + "-" + endLabel;
	}

	private static FavoriteRouteException invalidRequest(String message) {
		return invalidRequest(FavoriteRouteErrorCode.INVALID_FAVORITE_ROUTE_REQUEST, message);
	}

	private static FavoriteRouteException invalidRequest(FavoriteRouteErrorCode errorCode, String message) {
		return new FavoriteRouteException(errorCode, message);
	}
}
