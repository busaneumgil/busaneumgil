package com.ssafy.e102.domain.admin.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.e102.domain.admin.dto.request.AdminRoutePreviewRequest;
import com.ssafy.e102.domain.admin.dto.request.AdminRouteTuningRequest;
import com.ssafy.e102.domain.admin.dto.response.AdminRoutePreviewItemResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoutePreviewResponse;
import com.ssafy.e102.domain.route.type.WalkRouteProfile;
import com.ssafy.e102.global.exception.BusinessException;
import com.ssafy.e102.global.exception.CommonErrorCode;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteClient;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteRequest;
import com.ssafy.e102.global.geo.dto.GeoPointResponse;

@Service
@Transactional(readOnly = true)
public class AdminRoutePreviewService {

	private final GraphHopperRouteClient graphHopperRouteClient;
	private final ObjectMapper objectMapper;

	public AdminRoutePreviewService(GraphHopperRouteClient graphHopperRouteClient, ObjectMapper objectMapper) {
		this.graphHopperRouteClient = graphHopperRouteClient;
		this.objectMapper = objectMapper;
	}

	public AdminRoutePreviewResponse preview(AdminRoutePreviewRequest request) {
		WalkRouteProfile profile = parseProfile(request.profile());
		validateTuning(request.tuning());
		GraphHopperRouteRequest routeRequest = new GraphHopperRouteRequest(
			request.startPoint(),
			request.endPoint(),
			profile);
		GraphHopperRoutePath baseRoute = graphHopperRouteClient.route(routeRequest);
		GraphHopperRoutePath tunedRoute = graphHopperRouteClient.routeWithCustomModel(
			routeRequest,
			customModel(profile, request.tuning()));
		return new AdminRoutePreviewResponse(
			toResponse(profile, baseRoute),
			toResponse(profile, tunedRoute));
	}

	private WalkRouteProfile parseProfile(String profile) {
		try {
			return WalkRouteProfile.valueOf(profile.trim()
				.toUpperCase(Locale.ROOT));
		} catch (RuntimeException exception) {
			throw new BusinessException(CommonErrorCode.INVALID_INPUT, "지원하지 않는 경로 프로필입니다.", exception);
		}
	}

	private void validateTuning(AdminRouteTuningRequest tuning) {
		if (tuning.slopeLowPercent() >= tuning.slopeMiddlePercent()
			|| tuning.slopeMiddlePercent() >= tuning.slopeHighPercent()) {
			throw new BusinessException(CommonErrorCode.INVALID_INPUT, "경사도 임계값은 낮음 < 중간 < 높음 순서여야 합니다.");
		}
	}

	private JsonNode customModel(WalkRouteProfile profile, AdminRouteTuningRequest tuning) {
		ObjectNode root = objectMapper.createObjectNode();
		ArrayNode priority = root.putArray("priority");
		ProfileFixedPolicy fixedPolicy = fixedPolicy(profile);
		addPriority(priority, "walk_access == NO", 0.0);
		addPriority(priority, "walk_access == UNKNOWN", fixedPolicy.walkAccessUnknownPenalty());
		if (fixedPolicy.blockSlopeOver15()) {
			addPriority(priority, "avg_slope_percent >= 15.0", 0.0);
		}
		addPriority(priority, "stairs_state == YES", tuning.stairsPenalty());
		addPriority(priority, "stairs_state == UNKNOWN", fixedPolicy.stairsUnknownPenalty());
		addPriority(priority, slopeRange(tuning.slopeLowPercent(), tuning.slopeMiddlePercent()),
			tuning.slopeLowPenalty());
		addPriority(priority, slopeRange(tuning.slopeMiddlePercent(), tuning.slopeHighPercent()),
			tuning.slopeMiddlePenalty());
		addPriority(priority, highSlopeCondition(tuning.slopeHighPercent(), fixedPolicy.blockSlopeOver15()),
			tuning.slopeHighPenalty());
		if (fixedPolicy.widthAdequatePenalty() != null) {
			addPriority(priority, "width_state == ADEQUATE_120", fixedPolicy.widthAdequatePenalty());
		}
		addPriority(priority, "width_state == NARROW", tuning.narrowWidthPenalty());
		if (fixedPolicy.widthUnknownPenalty() != null) {
			addPriority(priority, "width_state == UNKNOWN", fixedPolicy.widthUnknownPenalty());
		}
		addPriority(priority, "surface_state == UNPAVED", tuning.unpavedSurfacePenalty());
		addPriority(priority, "surface_state == UNKNOWN", fixedPolicy.surfaceUnknownPenalty());
		addPriority(priority, "segment_type == CROSS_WALK && signal_state == YES", tuning.signalCrosswalkBonus());
		addPriority(priority, "segment_type == CROSS_WALK && signal_state == NO", fixedPolicy.crosswalkSignalNoPenalty());
		addPriority(priority, "segment_type == CROSS_WALK && signal_state == UNKNOWN",
			fixedPolicy.crosswalkSignalUnknownPenalty());
		addProfileSpecificRules(profile, priority);

		ArrayNode speed = root.putArray("speed");
		ObjectNode defaultSpeed = speed.addObject();
		defaultSpeed.put("if", "true");
		defaultSpeed.put("limit_to", number(defaultSpeed(profile)));
		root.put("distance_influence", tuning.distanceInfluence());
		return root;
	}

	private String slopeRange(double from, double to) {
		return "avg_slope_percent >= " + number(from) + " && avg_slope_percent < " + number(to);
	}

	private String highSlopeCondition(double from, boolean blockSlopeOver15) {
		if (blockSlopeOver15) {
			return "avg_slope_percent >= " + number(from) + " && avg_slope_percent < 15.0";
		}
		return "avg_slope_percent >= " + number(from);
	}

	private void addPriority(ArrayNode priority, String condition, double multiplier) {
		ObjectNode item = priority.addObject();
		item.put("if", condition);
		item.put("multiply_by", number(multiplier));
	}

	private double defaultSpeed(WalkRouteProfile profile) {
		return switch (profile) {
			case WHEELCHAIR_MANUAL_SAFE, WHEELCHAIR_MANUAL_FAST -> 1.80;
			case WHEELCHAIR_AUTO_SAFE, WHEELCHAIR_AUTO_FAST -> 4.32;
			case VISUAL_SAFE, VISUAL_FAST -> 2.88;
			case PEDESTRIAN_SAFE, PEDESTRIAN_FAST -> 4.68;
		};
	}

	private ProfileFixedPolicy fixedPolicy(WalkRouteProfile profile) {
		return switch (profile) {
			case PEDESTRIAN_SAFE -> new ProfileFixedPolicy(0.85, 0.90, null, 0.95, 0.90, 0.85, 0.90, false);
			case PEDESTRIAN_FAST -> new ProfileFixedPolicy(0.95, 0.95, null, null, 0.95, 0.90, 0.95, false);
			case VISUAL_SAFE -> new ProfileFixedPolicy(0.80, 0.85, 0.95, 0.90, 0.85, 0.60, 0.80, false);
			case VISUAL_FAST -> new ProfileFixedPolicy(0.90, 0.90, null, null, 0.95, 0.90, 0.95, false);
			case WHEELCHAIR_MANUAL_SAFE -> new ProfileFixedPolicy(0.65, 0.60, 0.65, 0.75, 0.70, 0.75, 0.85, true);
			case WHEELCHAIR_MANUAL_FAST -> new ProfileFixedPolicy(0.80, 0.75, 0.80, 0.90, 0.85, 0.90, 0.95, true);
			case WHEELCHAIR_AUTO_SAFE -> new ProfileFixedPolicy(0.75, 0.70, 0.75, 0.80, 0.80, 0.80, 0.90, false);
			case WHEELCHAIR_AUTO_FAST -> new ProfileFixedPolicy(0.85, 0.80, 0.90, 0.95, 0.90, 0.90, 0.95, false);
		};
	}

	private void addProfileSpecificRules(WalkRouteProfile profile, ArrayNode priority) {
		switch (profile) {
			case VISUAL_SAFE -> {
				addPriority(priority, "braille_block_state == YES", 1.08);
				addPriority(priority, "braille_block_state == NO", 0.90);
				addPriority(priority, "braille_block_state == UNKNOWN", 0.95);
				addPriority(priority, "audio_signal_state == YES", 1.10);
				addPriority(priority, "audio_signal_state == NO", 0.85);
				addPriority(priority, "audio_signal_state == UNKNOWN", 0.95);
			}
			case VISUAL_FAST -> {
				addPriority(priority, "braille_block_state == YES", 1.05);
				addPriority(priority, "braille_block_state == NO", 0.95);
				addPriority(priority, "braille_block_state == UNKNOWN", 0.98);
			}
			default -> {
			}
		}
	}

	private String number(double value) {
		return String.format(Locale.ROOT, "%.2f", value)
			.replaceAll("0+$", "")
			.replaceAll("\\.$", ".0");
	}

	private AdminRoutePreviewItemResponse toResponse(WalkRouteProfile profile, GraphHopperRoutePath route) {
		return new AdminRoutePreviewItemResponse(
			profile.name(),
			route.distanceMeter(),
			(int)(route.timeMs() / 1000),
			(int)Math.ceil(route.timeMs() / 60_000.0),
			route.coordinates()
				.stream()
				.map(coordinate -> new GeoPointResponse(coordinate.lat().doubleValue(), coordinate.lng().doubleValue()))
				.toList());
	}

	private record ProfileFixedPolicy(
		double walkAccessUnknownPenalty,
		double stairsUnknownPenalty,
		Double widthAdequatePenalty,
		Double widthUnknownPenalty,
		double surfaceUnknownPenalty,
		double crosswalkSignalNoPenalty,
		double crosswalkSignalUnknownPenalty,
		boolean blockSlopeOver15) {
	}
}
