package com.ssafy.e102.domain.route.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;

import com.ssafy.e102.domain.route.dto.response.RouteGuidanceEventResponse;
import com.ssafy.e102.domain.route.dto.response.RouteGuidanceEventType;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.domain.route.type.WalkRouteProfile;
import com.ssafy.e102.global.external.graphhopper.GraphHopperCoordinate;
import com.ssafy.e102.global.external.graphhopper.GraphHopperPathDetail;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;
import com.ssafy.e102.global.geo.GeoDistanceCalculator;

/**
 * GraphHopper path를 경로 API 응답 DTO로 변환한다.
 *
 * <p>WALK leg 전체 geometry를 유지하고, 회전/접근성 안내는 leg 시작점 기준 누적 거리의 guidanceEvents로 만든다.
 */
@Service
public class WalkRoutePayloadService {

	private static final String WALK_LEG_INSTRUCTION = "목적지까지 도보로 이동하세요.";
	private static final BigDecimal TURN_ZIGZAG_SUPPRESSION_METER = BigDecimal.valueOf(5);
	private static final List<RouteBadge> BADGE_PRIORITY = List.of(
		RouteBadge.STAIR,
		RouteBadge.NARROW_SIDEWALK,
		RouteBadge.UNPAVED,
		RouteBadge.MIDDLE_SLOPE,
		RouteBadge.LOW_SLOPE,
		RouteBadge.CROSSWALK,
		RouteBadge.ELEVATOR);
	private static final List<AlertRule> ALERT_RULES = List.of(
		new AlertRule(RouteGuidanceEventType.STAIR, "stairs_state", Set.of("YES"), 1),
		new AlertRule(RouteGuidanceEventType.NARROW_SIDEWALK, "width_state", Set.of("NARROW"), 2),
		new AlertRule(RouteGuidanceEventType.UNPAVED, "surface_state", Set.of("UNPAVED"), 3));

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
		List<RouteBadge> badges = badges(path, candidate.profile());
		List<RouteGuidanceEventResponse> guidanceEvents = toGuidanceEvents(
			path,
			candidate.profile(),
			distanceMeter,
			durationSecond,
			null,
			null);

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
			List.of(
				toWalkOnlyLeg(distanceMeter, durationSecond, estimatedTimeMinute, geometry, guidanceEvents, badges)));
	}

	public RouteLegResponse toWalkLeg(
		int sequence,
		RouteLegRole role,
		String instruction,
		GraphHopperRoutePath path) {
		return toWalkLeg(sequence, role, instruction, path, WalkRouteProfile.PEDESTRIAN_SAFE, null, null);
	}

	public RouteLegResponse toWalkLeg(
		int sequence,
		RouteLegRole role,
		String instruction,
		GraphHopperRoutePath path,
		RouteGuidanceEventType destinationEventType) {
		return toWalkLeg(sequence, role, instruction, path, WalkRouteProfile.PEDESTRIAN_SAFE, null,
			destinationEventType);
	}

	public RouteLegResponse toWalkLeg(
		int sequence,
		RouteLegRole role,
		String instruction,
		GraphHopperRoutePath path,
		RouteGuidanceEventType startEventType,
		RouteGuidanceEventType destinationEventType) {
		return toWalkLeg(sequence, role, instruction, path, WalkRouteProfile.PEDESTRIAN_SAFE, startEventType,
			destinationEventType);
	}

	public RouteLegResponse toWalkLeg(
		int sequence,
		RouteLegRole role,
		String instruction,
		GraphHopperRoutePath path,
		WalkRouteProfile profile,
		RouteGuidanceEventType startEventType,
		RouteGuidanceEventType destinationEventType) {
		String geometry = toLineString(path.coordinates());
		int durationSecond = durationSecond(path.timeMs());
		int estimatedTimeMinute = estimatedTimeMinute(durationSecond);
		BigDecimal distanceMeter = scaleDistance(path.distanceMeter());
		return new RouteLegResponse(
			sequence,
			TransportMode.WALK,
			role,
			instruction,
			distanceMeter,
			durationSecond,
			estimatedTimeMinute,
			geometry,
			toGuidanceEvents(path, profile, distanceMeter, durationSecond, startEventType, destinationEventType),
			null,
			List.of(),
			null,
			null,
			null,
			badges(path, profile));
	}

	private RouteLegResponse toWalkOnlyLeg(
		BigDecimal distanceMeter,
		int durationSecond,
		int estimatedTimeMinute,
		String geometry,
		List<RouteGuidanceEventResponse> guidanceEvents,
		List<RouteBadge> badges) {
		return new RouteLegResponse(
			1,
			TransportMode.WALK,
			RouteLegRole.WALK_ONLY,
			WALK_LEG_INSTRUCTION,
			distanceMeter,
			durationSecond,
			estimatedTimeMinute,
			geometry,
			guidanceEvents,
			null,
			List.of(),
			null,
			null,
			null,
			badges);
	}

	private List<RouteGuidanceEventResponse> toGuidanceEvents(
		GraphHopperRoutePath path,
		WalkRouteProfile profile,
		BigDecimal totalDistanceMeter,
		int totalDurationSecond,
		RouteGuidanceEventType startEventType,
		RouteGuidanceEventType destinationEventType) {
		List<GraphHopperCoordinate> coordinates = path.coordinates();
		if (coordinates.isEmpty()) {
			return List.of();
		}
		BigDecimal routeLength = routeLength(coordinates);
		List<GuidanceEventCandidate> candidates = new ArrayList<>();
		if (startEventType != null) {
			candidates.add(new GuidanceEventCandidate(
				startEventType,
				0,
				BigDecimal.ZERO.setScale(2),
				0,
				-1));
		}
		candidates.addAll(turnEventCandidates(path, totalDistanceMeter, routeLength));
		candidates.addAll(accessibilityEventCandidates(path, profile, totalDistanceMeter, routeLength));
		if (destinationEventType != null && coordinates.size() > 1) {
			int destinationIndex = coordinates.size() - 1;
			candidates.add(new GuidanceEventCandidate(
				destinationEventType,
				destinationIndex,
				scaledDistanceBetween(coordinates, 0, destinationIndex, totalDistanceMeter, routeLength),
				0,
				2));
		}

		List<GuidanceEventCandidate> sorted = candidates.stream()
			.sorted(Comparator
				.comparing(GuidanceEventCandidate::distanceFromLegStartMeter)
				.thenComparingInt(GuidanceEventCandidate::kindOrder)
				.thenComparingInt(GuidanceEventCandidate::priority)
				.thenComparing(candidate -> candidate.type().name()))
			.toList();

		List<RouteGuidanceEventResponse> events = new ArrayList<>();
		for (int index = 0; index < sorted.size(); index++) {
			GuidanceEventCandidate candidate = sorted.get(index);
			events.add(new RouteGuidanceEventResponse(
				index + 1,
				candidate.type(),
				candidate.distanceFromLegStartMeter(),
				durationFromLegStartSecond(candidate.distanceFromLegStartMeter(), totalDistanceMeter,
					totalDurationSecond),
				toPoint(coordinates.get(candidate.coordinateIndex()))));
		}
		return events;
	}

	private List<GuidanceEventCandidate> turnEventCandidates(
		GraphHopperRoutePath path,
		BigDecimal totalDistanceMeter,
		BigDecimal routeLength) {
		List<GraphHopperCoordinate> coordinates = path.coordinates();
		List<GuidanceEventCandidate> events = new ArrayList<>();
		for (int index = 1; index < coordinates.size() - 1; index++) {
			Optional<RouteTurnDirection> direction = turnDirection(path, index);
			if (direction.isPresent()) {
				events.add(new GuidanceEventCandidate(
					turnEventType(direction.get()),
					index,
					scaledDistanceBetween(coordinates, 0, index, totalDistanceMeter, routeLength),
					0,
					0));
			}
		}
		return suppressShortTurnZigzags(events);
	}

	private List<GuidanceEventCandidate> suppressShortTurnZigzags(List<GuidanceEventCandidate> events) {
		if (events.size() < 2) {
			return events;
		}
		Set<Integer> suppressedIndexes = new LinkedHashSet<>();
		for (int index = 0; index < events.size() - 1; index++) {
			GuidanceEventCandidate current = events.get(index);
			GuidanceEventCandidate next = events.get(index + 1);
			if (isTurn(current) && isTurn(next)
				&& next.distanceFromLegStartMeter()
					.subtract(current.distanceFromLegStartMeter())
					.abs()
					.compareTo(TURN_ZIGZAG_SUPPRESSION_METER) <= 0) {
				suppressedIndexes.add(index);
				suppressedIndexes.add(index + 1);
			}
		}
		List<GuidanceEventCandidate> filtered = new ArrayList<>();
		for (int index = 0; index < events.size(); index++) {
			if (!suppressedIndexes.contains(index)) {
				filtered.add(events.get(index));
			}
		}
		return filtered;
	}

	private boolean isTurn(GuidanceEventCandidate candidate) {
		return candidate.type() == RouteGuidanceEventType.TURN_LEFT
			|| candidate.type() == RouteGuidanceEventType.TURN_RIGHT;
	}

	private RouteGuidanceEventType turnEventType(RouteTurnDirection direction) {
		return switch (direction) {
			case LEFT -> RouteGuidanceEventType.TURN_LEFT;
			case RIGHT -> RouteGuidanceEventType.TURN_RIGHT;
		};
	}

	private List<GuidanceEventCandidate> accessibilityEventCandidates(
		GraphHopperRoutePath path,
		WalkRouteProfile profile,
		BigDecimal totalDistanceMeter,
		BigDecimal routeLength) {
		List<GuidanceEventCandidate> candidates = new ArrayList<>();
		candidates.addAll(crosswalkEventCandidates(path, totalDistanceMeter, routeLength));
		candidates.addAll(slopeEventCandidates(path, profile, totalDistanceMeter, routeLength));
		ALERT_RULES
			.forEach(rule -> candidates.addAll(alertEventCandidates(path, rule, totalDistanceMeter, routeLength)));

		Map<Integer, GuidanceEventCandidate> representativeByIndex = new LinkedHashMap<>();
		candidates.stream()
			.sorted(Comparator
				.comparingInt(GuidanceEventCandidate::priority)
				.thenComparing(GuidanceEventCandidate::distanceFromLegStartMeter))
			.forEach(candidate -> representativeByIndex.putIfAbsent(candidate.coordinateIndex(), candidate));
		return new ArrayList<>(representativeByIndex.values());
	}

	private List<GuidanceEventCandidate> crosswalkEventCandidates(
		GraphHopperRoutePath path,
		BigDecimal totalDistanceMeter,
		BigDecimal routeLength) {
		return path.details()
			.getOrDefault("segment_type", List.of())
			.stream()
			.filter(detail -> "CROSS_WALK".equals(detail.value()))
			.flatMap(
				detail -> eventDistanceMeter(path.coordinates(), detail.fromIndex(), totalDistanceMeter, routeLength)
					.map(distanceMeter -> new GuidanceEventCandidate(
						crosswalkEventType(path, detail),
						detail.fromIndex(),
						distanceMeter,
						crosswalkPriority(path, detail),
						1))
					.stream())
			.toList();
	}

	private List<GuidanceEventCandidate> slopeEventCandidates(
		GraphHopperRoutePath path,
		WalkRouteProfile profile,
		BigDecimal totalDistanceMeter,
		BigDecimal routeLength) {
		return path.details()
			.getOrDefault("avg_slope_percent", List.of())
			.stream()
			.flatMap(
				detail -> slopeGuidanceEventType(profile, detail.value())
					.flatMap(type -> eventDistanceMeter(
						path.coordinates(),
						detail.fromIndex(),
						totalDistanceMeter,
						routeLength)
						.map(distanceMeter -> new GuidanceEventCandidate(
							type,
							detail.fromIndex(),
							distanceMeter,
							slopePriority(type),
							1)))
					.stream())
			.toList();
	}

	private Optional<RouteGuidanceEventType> slopeGuidanceEventType(WalkRouteProfile profile, String value) {
		return slopeSeverity(profile, value)
			.map(severity -> severity == SlopeSeverity.LOW
				? RouteGuidanceEventType.LOW_SLOPE
				: RouteGuidanceEventType.MIDDLE_SLOPE);
	}

	private int slopePriority(RouteGuidanceEventType type) {
		return type == RouteGuidanceEventType.MIDDLE_SLOPE ? 4 : 5;
	}

	private RouteGuidanceEventType crosswalkEventType(
		GraphHopperRoutePath path,
		GraphHopperPathDetail crosswalkDetail) {
		boolean hasSignal = hasOverlappingDetailValue(
			path,
			"signal_state",
			"YES",
			crosswalkDetail,
			crosswalkDetail.fromIndex(),
			crosswalkDetail.toIndex());
		boolean hasAudioSignal = hasOverlappingDetailValue(
			path,
			"audio_signal_state",
			"YES",
			crosswalkDetail,
			crosswalkDetail.fromIndex(),
			crosswalkDetail.toIndex());
		if (hasSignal && hasAudioSignal) {
			return RouteGuidanceEventType.CROSSWALK_AUDIO;
		}
		if (hasSignal) {
			return RouteGuidanceEventType.CROSSWALK_SIGNAL;
		}
		return RouteGuidanceEventType.CROSSWALK;
	}

	private int crosswalkPriority(
		GraphHopperRoutePath path,
		GraphHopperPathDetail crosswalkDetail) {
		return switch (crosswalkEventType(path, crosswalkDetail)) {
			case CROSSWALK_AUDIO -> 6;
			case CROSSWALK_SIGNAL -> 7;
			case CROSSWALK -> 8;
			default -> throw new IllegalStateException("unexpected crosswalk event type");
		};
	}

	private List<GuidanceEventCandidate> alertEventCandidates(
		GraphHopperRoutePath path,
		AlertRule rule,
		BigDecimal totalDistanceMeter,
		BigDecimal routeLength) {
		return path.details()
			.getOrDefault(rule.detailName(), List.of())
			.stream()
			.filter(detail -> rule.expectedValues().contains(detail.value()))
			.flatMap(
				detail -> eventDistanceMeter(path.coordinates(), detail.fromIndex(), totalDistanceMeter, routeLength)
					.map(distanceMeter -> new GuidanceEventCandidate(
						rule.type(),
						detail.fromIndex(),
						distanceMeter,
						rule.priority(),
						1))
					.stream())
			.toList();
	}

	private Optional<BigDecimal> eventDistanceMeter(
		List<GraphHopperCoordinate> coordinates,
		int eventIndex,
		BigDecimal totalDistanceMeter,
		BigDecimal routeLength) {
		if (eventIndex < 0 || eventIndex >= coordinates.size()) {
			return Optional.empty();
		}
		return Optional.of(scaledDistanceBetween(coordinates, 0, eventIndex, totalDistanceMeter, routeLength));
	}

	private boolean hasOverlappingDetailValue(
		GraphHopperRoutePath path,
		String detailName,
		String expectedValue,
		GraphHopperPathDetail baseDetail,
		int fromIndex,
		int toIndex) {
		int overlapFrom = Math.max(baseDetail.fromIndex(), fromIndex);
		int overlapTo = Math.min(baseDetail.toIndex(), toIndex);
		return path.details()
			.getOrDefault(detailName, List.of())
			.stream()
			.anyMatch(detail -> expectedValue.equals(detail.value()) && detail.fromIndex() < overlapTo
				&& detail.toIndex() > overlapFrom);
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

	private BigDecimal scaledDistanceBetween(
		List<GraphHopperCoordinate> coordinates,
		int fromIndex,
		int toIndex,
		BigDecimal totalDistanceMeter,
		BigDecimal routeLength) {
		if (toIndex <= fromIndex || routeLength.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO.setScale(2);
		}
		BigDecimal distance = routeLength(coordinates.subList(fromIndex, toIndex + 1));
		BigDecimal ratio = distance.divide(routeLength, 8, RoundingMode.HALF_UP);
		return totalDistanceMeter.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
	}

	private int durationFromLegStartSecond(
		BigDecimal distanceFromLegStartMeter,
		BigDecimal totalDistanceMeter,
		int totalDurationSecond) {
		if (totalDistanceMeter.compareTo(BigDecimal.ZERO) == 0) {
			return 0;
		}
		return BigDecimal.valueOf(totalDurationSecond)
			.multiply(distanceFromLegStartMeter)
			.divide(totalDistanceMeter, 8, RoundingMode.HALF_UP)
			.setScale(0, RoundingMode.HALF_UP)
			.intValue();
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

	private List<RouteBadge> badges(GraphHopperRoutePath path, WalkRouteProfile profile) {
		Set<RouteBadge> badges = new LinkedHashSet<>();
		if (hasSlopeSeverity(path, profile, SlopeSeverity.MIDDLE, SlopeSeverity.HIGH)) {
			badges.add(RouteBadge.MIDDLE_SLOPE);
		}
		if (hasSlopeSeverity(path, profile, SlopeSeverity.LOW)) {
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
		return BADGE_PRIORITY.stream()
			.filter(badges::contains)
			.toList();
	}

	private boolean hasSlopeSeverity(GraphHopperRoutePath path, WalkRouteProfile profile, SlopeSeverity... severities) {
		Set<SlopeSeverity> expected = Set.of(severities);
		return path.details()
			.getOrDefault("avg_slope_percent", List.of())
			.stream()
			.map(detail -> slopeSeverity(profile, detail.value()))
			.flatMap(Optional::stream)
			.anyMatch(expected::contains);
	}

	private Optional<SlopeSeverity> slopeSeverity(WalkRouteProfile profile, String value) {
		BigDecimal slopePercent = parseSlopePercent(value);
		if (slopePercent == null || slopePercent.compareTo(BigDecimal.ZERO) < 0) {
			return Optional.empty();
		}
		SlopeThreshold threshold = slopeThreshold(profile);
		if (slopePercent.compareTo(threshold.lowUpperExclusive()) < 0) {
			return Optional.of(SlopeSeverity.LOW);
		}
		if (slopePercent.compareTo(threshold.middleUpperExclusive()) < 0) {
			return Optional.of(SlopeSeverity.MIDDLE);
		}
		return Optional.of(SlopeSeverity.HIGH);
	}

	private SlopeThreshold slopeThreshold(WalkRouteProfile profile) {
		WalkRouteProfile effectiveProfile = profile == null ? WalkRouteProfile.PEDESTRIAN_SAFE : profile;
		return switch (effectiveProfile) {
			case PEDESTRIAN_SAFE, PEDESTRIAN_FAST, WHEELCHAIR_AUTO_SAFE, WHEELCHAIR_AUTO_FAST ->
				new SlopeThreshold(new BigDecimal("5.56"), new BigDecimal("8.33"));
			case VISUAL_SAFE, VISUAL_FAST, WHEELCHAIR_MANUAL_SAFE, WHEELCHAIR_MANUAL_FAST ->
				new SlopeThreshold(new BigDecimal("3.00"), new BigDecimal("5.56"));
		};
	}

	private BigDecimal parseSlopePercent(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return new BigDecimal(value.trim());
		} catch (NumberFormatException exception) {
			return null;
		}
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
			default -> throw new IllegalArgumentException("Unsupported walk route option: " + routeOption);
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

	private String toPoint(GraphHopperCoordinate coordinate) {
		return "POINT(" + coordinate.lng().toPlainString() + " " + coordinate.lat().toPlainString() + ")";
	}

	private record AlertRule(
		RouteGuidanceEventType type,
		String detailName,
		Set<String> expectedValues,
		int priority) {
	}

	private record GuidanceEventCandidate(
		RouteGuidanceEventType type,
		int coordinateIndex,
		BigDecimal distanceFromLegStartMeter,
		int priority,
		int kindOrder) {
	}

	private record SlopeThreshold(
		BigDecimal lowUpperExclusive,
		BigDecimal middleUpperExclusive) {
	}

	private enum SlopeSeverity {
		LOW,
		MIDDLE,
		HIGH
	}
}
