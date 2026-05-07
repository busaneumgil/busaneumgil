package com.ssafy.e102.domain.route.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteStepAlertResponse;
import com.ssafy.e102.domain.route.dto.response.RouteStepAlertType;
import com.ssafy.e102.domain.route.dto.response.RouteStepResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.domain.route.type.WidthState;
import com.ssafy.e102.global.external.graphhopper.GraphHopperCoordinate;
import com.ssafy.e102.global.external.graphhopper.GraphHopperPathDetail;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;

/**
 * GraphHopper path를 경로 API 응답 DTO로 변환한다.
 *
 * <p>이 서비스는 GraphHopper 원본 JSON 대신 {@link GraphHopperRoutePath}의 정제된 거리, 시간, geometry, path detail만
 * 읽고 route/leg/step payload를 만든다.
 */
@Service
public class WalkRoutePayloadService {

	private static final String WALK_LEG_INSTRUCTION = "목적지까지 도보로 이동하세요.";

	public RouteSummaryResponse toRouteSummary(String searchId, WalkRouteCandidate candidate) {
		GraphHopperRoutePath path = candidate.path();
		String geometry = toLineString(path.coordinates());
		int durationSecond = durationSecond(path.timeMs());
		int estimatedTimeMinute = estimatedTimeMinute(durationSecond);
		BigDecimal distanceMeter = scaleDistance(path.distanceMeter());
		List<RouteBadge> badges = badges(path);
		List<RouteStepResponse> steps = List.of(toSingleStep(path, distanceMeter, durationSecond, geometry, badges));

		return new RouteSummaryResponse(
			routeId(searchId, candidate.routeOption()),
			TransportMode.WALK,
			candidate.routeOption(),
			title(candidate.routeOption()),
			distanceMeter,
			durationSecond,
			estimatedTimeMinute,
			badges,
			geometry,
			List.of(toWalkOnlyLeg(distanceMeter, durationSecond, estimatedTimeMinute, geometry, steps)));
	}

	private RouteLegResponse toWalkOnlyLeg(
		BigDecimal distanceMeter,
		int durationSecond,
		int estimatedTimeMinute,
		String geometry,
		List<RouteStepResponse> steps) {
		return new RouteLegResponse(
			1,
			TransportMode.WALK,
			RouteLegRole.WALK_ONLY,
			WALK_LEG_INSTRUCTION,
			distanceMeter,
			durationSecond,
			estimatedTimeMinute,
			geometry,
			steps);
	}

	private RouteStepResponse toSingleStep(
		GraphHopperRoutePath path,
		BigDecimal distanceMeter,
		int durationSecond,
		String geometry,
		List<RouteBadge> badges) {
		RouteStepAlertResponse alert = representativeAlert(path)
			.map(type -> new RouteStepAlertResponse(type, BigDecimal.ZERO.setScale(2)))
			.orElse(null);
		return new RouteStepResponse(
			1,
			instruction(alert),
			distanceMeter,
			durationSecond,
			geometry,
			badges,
			alert,
			averageDecimalDetail(path, "avg_slope_percent").orElse(null),
			widthState(path).orElse(null));
	}

	private String instruction(RouteStepAlertResponse alert) {
		if (alert == null) {
			return "경로를 따라 이동하세요.";
		}
		return switch (alert.type()) {
			case CROSSWALK -> "횡단보도를 건너세요.";
			case MIDDLE_SLOPE -> "경사 구간을 이동하세요.";
			case STAIR -> "계단 구간을 이동하세요.";
			case NARROW_SIDEWALK -> "좁은 보도 구간을 이동하세요.";
			case UNPAVED -> "포장되지 않은 구간을 이동하세요.";
			default -> "경로를 따라 이동하세요.";
		};
	}

	private List<RouteBadge> badges(GraphHopperRoutePath path) {
		Set<RouteBadge> badges = new LinkedHashSet<>();
		if (hasAny(path, "slope_state", "MODERATE", "STEEP", "RISK")) {
			badges.add(RouteBadge.MIDDLE_SLOPE);
		} else if (hasAny(path, "slope_state", "FLAT")) {
			badges.add(RouteBadge.LOW_SLOPE);
		}
		if (hasAny(path, "stairs_state", "YES")) {
			badges.add(RouteBadge.STAIR);
		}
		if (hasAny(path, "segment_type", "CROSS_WALK")) {
			badges.add(RouteBadge.CROSSWALK);
		}
		if (hasAny(path, "width_state", "NARROW")) {
			badges.add(RouteBadge.NARROW_SIDEWALK);
		}
		if (hasAny(path, "surface_state", "UNPAVED")) {
			badges.add(RouteBadge.UNPAVED);
		}
		return new ArrayList<>(badges);
	}

	private Optional<RouteStepAlertType> representativeAlert(GraphHopperRoutePath path) {
		if (hasAny(path, "segment_type", "CROSS_WALK") && hasAny(path, "signal_state", "YES")) {
			return Optional.of(RouteStepAlertType.CROSSWALK);
		}
		if (hasAny(path, "stairs_state", "YES")) {
			return Optional.of(RouteStepAlertType.STAIR);
		}
		if (hasAny(path, "slope_state", "MODERATE", "STEEP", "RISK")) {
			return Optional.of(RouteStepAlertType.MIDDLE_SLOPE);
		}
		if (hasAny(path, "width_state", "NARROW")) {
			return Optional.of(RouteStepAlertType.NARROW_SIDEWALK);
		}
		if (hasAny(path, "surface_state", "UNPAVED")) {
			return Optional.of(RouteStepAlertType.UNPAVED);
		}
		return Optional.empty();
	}

	private Optional<BigDecimal> averageDecimalDetail(GraphHopperRoutePath path, String detailName) {
		List<BigDecimal> values = detailValues(path, detailName)
			.stream()
			.map(this::parseDecimal)
			.flatMap(Optional::stream)
			.toList();
		if (values.isEmpty()) {
			return Optional.empty();
		}
		BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return Optional.of(sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP));
	}

	private Optional<WidthState> widthState(GraphHopperRoutePath path) {
		List<String> values = detailValues(path, "width_state");
		if (values.contains("NARROW")) {
			return Optional.of(WidthState.NARROW);
		}
		if (values.contains("ADEQUATE_120")) {
			return Optional.of(WidthState.ADEQUATE_120);
		}
		if (values.contains("ADEQUATE_150")) {
			return Optional.of(WidthState.ADEQUATE_150);
		}
		if (values.contains("UNKNOWN")) {
			return Optional.of(WidthState.UNKNOWN);
		}
		return Optional.empty();
	}

	private boolean hasAny(GraphHopperRoutePath path, String detailName, String... expectedValues) {
		Set<String> expected = Set.of(expectedValues);
		return detailValues(path, detailName)
			.stream()
			.anyMatch(expected::contains);
	}

	private List<String> detailValues(GraphHopperRoutePath path, String detailName) {
		return path.details()
			.getOrDefault(detailName, List.of())
			.stream()
			.map(GraphHopperPathDetail::value)
			.toList();
	}

	private Optional<BigDecimal> parseDecimal(String value) {
		try {
			return Optional.of(new BigDecimal(value));
		} catch (NumberFormatException exception) {
			return Optional.empty();
		}
	}

	private String routeId(String searchId, RouteOption routeOption) {
		return searchId + "_" + routeOption.name().toLowerCase();
	}

	private String title(RouteOption routeOption) {
		return switch (routeOption) {
			case SAFE -> "안전 경로";
			case SHORTEST -> "최단 경로";
		};
	}

	private int durationSecond(long timeMs) {
		return Math.max(1, (int)Math.ceil(timeMs / 1000.0));
	}

	private int estimatedTimeMinute(int durationSecond) {
		return Math.max(1, durationSecond / 60);
	}

	private BigDecimal scaleDistance(BigDecimal distanceMeter) {
		return distanceMeter.setScale(2, RoundingMode.HALF_UP);
	}

	private String toLineString(List<GraphHopperCoordinate> coordinates) {
		String points = coordinates.stream()
			.map(coordinate -> coordinate.lng().toPlainString() + " " + coordinate.lat().toPlainString())
			.collect(Collectors.joining(", "));
		return "LINESTRING(" + points + ")";
	}
}
