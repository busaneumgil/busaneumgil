package com.ssafy.e102.domain.route.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
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
import com.ssafy.e102.global.external.graphhopper.GraphHopperCoordinate;
import com.ssafy.e102.global.external.graphhopper.GraphHopperPathDetail;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;
import com.ssafy.e102.global.geo.GeoDistanceCalculator;

/**
 * GraphHopper path를 경로 API 응답 DTO로 변환한다.
 *
 * <p>이 서비스는 GraphHopper 원본 JSON 대신 {@link GraphHopperRoutePath}의 정제된 거리, 시간, geometry, path detail만
 * 읽고 route/leg/step payload를 만든다.
 */
@Service
public class WalkRoutePayloadService {

	private static final String WALK_LEG_INSTRUCTION = "목적지까지 도보로 이동하세요.";
	private static final List<AlertRule> ALERT_RULES = List.of(
		new AlertRule(RouteStepAlertType.CROSSWALK, "segment_type", Set.of("CROSS_WALK"), 1),
		new AlertRule(RouteStepAlertType.STAIR, "stairs_state", Set.of("YES"), 2),
		new AlertRule(RouteStepAlertType.NARROW_SIDEWALK, "width_state", Set.of("NARROW"), 3),
		new AlertRule(RouteStepAlertType.UNPAVED, "surface_state", Set.of("UNPAVED"), 4),
		new AlertRule(RouteStepAlertType.MIDDLE_SLOPE, "slope_state", Set.of("MODERATE", "STEEP", "RISK"), 5));

	private final RouteTurnInstructionService routeTurnInstructionService;

	public WalkRoutePayloadService(RouteTurnInstructionService routeTurnInstructionService) {
		this.routeTurnInstructionService = routeTurnInstructionService;
	}

	public RouteSummaryResponse toRouteSummary(String searchId, WalkRouteCandidate candidate) {
		GraphHopperRoutePath path = candidate.path();
		String geometry = toLineString(path.coordinates());
		int durationSecond = durationSecond(path.timeMs());
		int estimatedTimeMinute = estimatedTimeMinute(durationSecond);
		BigDecimal distanceMeter = scaleDistance(path.distanceMeter());
		List<RouteBadge> badges = badges(path);
		List<RouteStepResponse> steps = toSteps(path, distanceMeter, durationSecond);

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

	private List<RouteStepResponse> toSteps(GraphHopperRoutePath path, BigDecimal totalDistanceMeter,
		int totalDurationSecond) {
		List<GraphHopperCoordinate> coordinates = path.coordinates();
		if (coordinates.size() < 2) {
			return List.of(toStep(path, 1, 0, coordinates.size() - 1, totalDistanceMeter, totalDurationSecond));
		}
		List<Integer> splitPoints = splitPoints(path);
		BigDecimal routeLength = routeLength(coordinates);
		List<RouteStepResponse> steps = new ArrayList<>();
		BigDecimal accumulatedDistance = BigDecimal.ZERO.setScale(2);
		int accumulatedDuration = 0;
		for (int index = 0; index < splitPoints.size() - 1; index++) {
			int fromIndex = splitPoints.get(index);
			int toIndex = splitPoints.get(index + 1);
			boolean isLastStep = index == splitPoints.size() - 2;
			BigDecimal stepDistance = stepDistance(
				totalDistanceMeter,
				coordinates,
				fromIndex,
				toIndex,
				routeLength,
				accumulatedDistance,
				isLastStep);
			int stepDuration = stepDuration(
				totalDurationSecond,
				coordinates,
				fromIndex,
				toIndex,
				routeLength,
				accumulatedDuration,
				isLastStep);
			accumulatedDistance = accumulatedDistance.add(stepDistance);
			accumulatedDuration += stepDuration;
			steps.add(toStep(path, index + 1, fromIndex, toIndex, stepDistance, stepDuration));
		}
		return steps;
	}

	private RouteStepResponse toStep(
		GraphHopperRoutePath path,
		int sequence,
		int fromIndex,
		int toIndex,
		BigDecimal distanceMeter,
		int durationSecond) {
		GraphHopperRoutePath stepPath = slice(path, fromIndex, toIndex);
		Optional<RouteTurnDirection> turnDirection = turnDirection(path, fromIndex);
		RouteStepAlertResponse alert = representativeAlert(path, fromIndex, toIndex, distanceMeter)
			.map(candidate -> new RouteStepAlertResponse(candidate.type(), candidate.distanceMeter()))
			.orElse(null);
		return new RouteStepResponse(
			sequence,
			instruction(turnDirection),
			distanceMeter,
			durationSecond,
			toLineString(stepPath.coordinates()),
			alert);
	}

	private String instruction(Optional<RouteTurnDirection> turnDirection) {
		return turnDirection
			.map(RouteTurnDirection::instruction)
			.orElse("직진하세요.");
	}

	private List<Integer> splitPoints(GraphHopperRoutePath path) {
		int lastCoordinateIndex = path.coordinates().size() - 1;
		SortedSet<Integer> splitPoints = new TreeSet<>();
		splitPoints.add(0);
		splitPoints.add(lastCoordinateIndex);
		path.details().values()
			.stream()
			.flatMap(List::stream)
			.forEach(detail -> {
				addSplitPoint(splitPoints, detail.fromIndex(), lastCoordinateIndex);
				addSplitPoint(splitPoints, detail.toIndex(), lastCoordinateIndex);
			});
		for (int index = 1; index < lastCoordinateIndex; index++) {
			if (turnDirection(path, index).isPresent()) {
				splitPoints.add(index);
			}
		}
		return new ArrayList<>(splitPoints);
	}

	private void addSplitPoint(SortedSet<Integer> splitPoints, int index, int lastCoordinateIndex) {
		if (index > 0 && index < lastCoordinateIndex) {
			splitPoints.add(index);
		}
	}

	private GraphHopperRoutePath slice(GraphHopperRoutePath path, int fromIndex, int toIndex) {
		int normalizedFrom = Math.max(0, fromIndex);
		int normalizedTo = Math.min(path.coordinates().size() - 1, Math.max(toIndex, normalizedFrom));
		List<GraphHopperCoordinate> coordinates = path.coordinates().subList(normalizedFrom, normalizedTo + 1);
		return new GraphHopperRoutePath(
			BigDecimal.ZERO,
			0,
			coordinates,
			path.details()
				.entrySet()
				.stream()
				.collect(Collectors.toMap(
					java.util.Map.Entry::getKey,
					entry -> overlappingDetails(entry.getValue(), normalizedFrom, normalizedTo))));
	}

	private List<GraphHopperPathDetail> overlappingDetails(
		List<GraphHopperPathDetail> details,
		int fromIndex,
		int toIndex) {
		return details.stream()
			.filter(detail -> detail.fromIndex() < toIndex && detail.toIndex() > fromIndex)
			.toList();
	}

	private Optional<RouteTurnDirection> turnDirection(GraphHopperRoutePath path, int pivotIndex) {
		if (pivotIndex <= 0 || pivotIndex >= path.coordinates().size() - 1) {
			return Optional.empty();
		}
		return routeTurnInstructionService.resolve(
			toCoordinate(path.coordinates().get(pivotIndex - 1)),
			toCoordinate(path.coordinates().get(pivotIndex)),
			toCoordinate(path.coordinates().get(pivotIndex + 1)));
	}

	private Coordinate toCoordinate(GraphHopperCoordinate coordinate) {
		return new Coordinate(coordinate.lng().doubleValue(), coordinate.lat().doubleValue());
	}

	private BigDecimal stepDistance(
		BigDecimal totalDistanceMeter,
		List<GraphHopperCoordinate> coordinates,
		int fromIndex,
		int toIndex,
		BigDecimal routeLength,
		BigDecimal accumulatedDistance,
		boolean isLastStep) {
		if (isLastStep) {
			return totalDistanceMeter.subtract(accumulatedDistance)
				.max(BigDecimal.ZERO)
				.setScale(2, RoundingMode.HALF_UP);
		}
		BigDecimal ratio = stepLengthRatio(coordinates, fromIndex, toIndex, routeLength);
		return totalDistanceMeter.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
	}

	private int stepDuration(
		int totalDurationSecond,
		List<GraphHopperCoordinate> coordinates,
		int fromIndex,
		int toIndex,
		BigDecimal routeLength,
		int accumulatedDuration,
		boolean isLastStep) {
		if (isLastStep) {
			return Math.max(1, totalDurationSecond - accumulatedDuration);
		}
		BigDecimal ratio = stepLengthRatio(coordinates, fromIndex, toIndex, routeLength);
		return Math.max(1, BigDecimal.valueOf(totalDurationSecond)
			.multiply(ratio)
			.setScale(0, RoundingMode.HALF_UP)
			.intValue());
	}

	private BigDecimal stepLengthRatio(
		List<GraphHopperCoordinate> coordinates,
		int fromIndex,
		int toIndex,
		BigDecimal routeLength) {
		if (routeLength.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ONE;
		}
		return routeLength(coordinates.subList(fromIndex, toIndex + 1)).divide(routeLength, 8, RoundingMode.HALF_UP);
	}

	private BigDecimal routeLength(List<GraphHopperCoordinate> coordinates) {
		if (coordinates.size() < 2) {
			return BigDecimal.ZERO;
		}
		BigDecimal length = BigDecimal.ZERO;
		for (int index = 0; index < coordinates.size() - 1; index++) {
			length = length.add(segmentLength(coordinates.get(index), coordinates.get(index + 1)));
		}
		return length;
	}

	private BigDecimal segmentLength(GraphHopperCoordinate from, GraphHopperCoordinate to) {
		return BigDecimal.valueOf(GeoDistanceCalculator.distanceMeter(
			from.lat().doubleValue(),
			from.lng().doubleValue(),
			to.lat().doubleValue(),
			to.lng().doubleValue()));
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

	private Optional<StepAlertCandidate> representativeAlert(
		GraphHopperRoutePath path,
		int fromIndex,
		int toIndex,
		BigDecimal stepDistanceMeter) {
		BigDecimal stepLength = routeLength(path.coordinates().subList(fromIndex, toIndex + 1));
		return ALERT_RULES.stream()
			.flatMap(rule -> alertCandidates(path, rule, fromIndex, toIndex, stepDistanceMeter, stepLength).stream())
			.min(Comparator
				.comparingInt(StepAlertCandidate::priority)
				.thenComparing(StepAlertCandidate::distanceMeter));
	}

	private List<StepAlertCandidate> alertCandidates(
		GraphHopperRoutePath path,
		AlertRule rule,
		int fromIndex,
		int toIndex,
		BigDecimal stepDistanceMeter,
		BigDecimal stepLength) {
		return path.details()
			.getOrDefault(rule.detailName(), List.of())
			.stream()
			.filter(detail -> rule.expectedValues().contains(detail.value()))
			.filter(detail -> isOverlapping(detail, fromIndex, toIndex))
			.map(detail -> new StepAlertCandidate(
				rule.type(),
				relativeDistanceMeter(path.coordinates(), fromIndex, toIndex, detail.fromIndex(), stepDistanceMeter,
					stepLength),
				rule.priority()))
			.toList();
	}

	private boolean isOverlapping(GraphHopperPathDetail detail, int fromIndex, int toIndex) {
		return detail.fromIndex() < toIndex && detail.toIndex() > fromIndex;
	}

	private BigDecimal relativeDistanceMeter(
		List<GraphHopperCoordinate> coordinates,
		int stepFromIndex,
		int stepToIndex,
		int eventFromIndex,
		BigDecimal stepDistanceMeter,
		BigDecimal stepLength) {
		int normalizedEventIndex = Math.min(Math.max(eventFromIndex, stepFromIndex), stepToIndex);
		if (normalizedEventIndex <= stepFromIndex || stepLength.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO.setScale(2);
		}
		BigDecimal eventOffsetLength = routeLength(coordinates.subList(stepFromIndex, normalizedEventIndex + 1));
		BigDecimal ratio = eventOffsetLength.divide(stepLength, 8, RoundingMode.HALF_UP);
		return stepDistanceMeter.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
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

	private record AlertRule(
		RouteStepAlertType type,
		String detailName,
		Set<String> expectedValues,
		int priority) {
	}

	private record StepAlertCandidate(
		RouteStepAlertType type,
		BigDecimal distanceMeter,
		int priority) {
	}
}
