package com.ssafy.e102.domain.route.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.route.dto.response.RouteGuidanceEventResponse;
import com.ssafy.e102.domain.route.dto.response.RouteGuidanceEventType;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;

class RouteDtoJsonTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("walk route response serializes guidanceEvents API contract")
	void walkRouteSearchResponseSerializesApiContract() throws Exception {
		WalkRouteSearchResponse response = new WalkRouteSearchResponse(
			"rs_walk_20260506_abc123",
			List.of(new RouteSummaryResponse(
				"walk_rt_safe_001",
				TransportMode.WALK,
				RouteOption.SAFE,
				"safe route",
				BigDecimal.valueOf(950),
				960,
				16,
				List.of(RouteBadge.LOW_SLOPE, RouteBadge.CROSSWALK),
				"LINESTRING(128.9360 35.1200, 128.8823 35.1315)",
				List.of(new RouteLegResponse(
					1,
					TransportMode.WALK,
					RouteLegRole.WALK_ONLY,
					"walk to destination",
					BigDecimal.valueOf(950),
					960,
					16,
					"LINESTRING(128.9360 35.1200, 128.8823 35.1315)",
					List.of(new RouteGuidanceEventResponse(
						1,
						RouteGuidanceEventType.CROSSWALK_AUDIO,
						BigDecimal.valueOf(12),
						35,
						"POINT(128.9360 35.1200)")))))));

		JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(response));

		assertThat(root.get("searchId").asText()).isEqualTo("rs_walk_20260506_abc123");
		JsonNode route = root.get("routes").get(0);
		assertThat(route.get("routeId").asText()).isEqualTo("walk_rt_safe_001");
		assertThat(route.get("transportMode").asText()).isEqualTo("WALK");
		assertThat(route.get("routeOption").asText()).isEqualTo("SAFE");
		assertThat(route.get("routeOptions").get(0).asText()).isEqualTo("SAFE");
		assertThat(route.has("transferCount")).isFalse();
		assertThat(route.get("badges").get(0).asText()).isEqualTo("LOW_SLOPE");
		JsonNode leg = route.get("legs").get(0);
		assertThat(leg.get("type").asText()).isEqualTo("WALK");
		assertThat(leg.get("role").asText()).isEqualTo("WALK_ONLY");
		assertThat(leg.has("routeNo")).isFalse();
		assertThat(leg.has("laneOptions")).isFalse();
		assertThat(leg.has("boardingStop")).isFalse();
		assertThat(leg.has("arrivingStop")).isFalse();
		assertThat(leg.has("alightingStop")).isFalse();
		assertThat(leg.has("isLowFloor")).isFalse();
		assertThat(leg.has("badges")).isFalse();
		assertThat(leg.has("steps")).isFalse();
		JsonNode guidanceEvent = leg.get("guidanceEvents").get(0);
		assertThat(guidanceEvent.get("sequence").asInt()).isEqualTo(1);
		assertThat(guidanceEvent.get("type").asText()).isEqualTo("CROSSWALK_AUDIO");
		assertThat(guidanceEvent.get("distanceFromLegStartMeter").decimalValue()).isEqualByComparingTo("12");
		assertThat(guidanceEvent.get("durationFromLegStartSecond").asInt()).isEqualTo(35);
		assertThat(guidanceEvent.get("distanceFromRouteStartMeter").decimalValue()).isEqualByComparingTo("12");
		assertThat(guidanceEvent.get("durationFromRouteStartSecond").asInt()).isEqualTo(35);
		assertThat(guidanceEvent.get("geometry").asText()).isEqualTo("POINT(128.9360 35.1200)");
	}

	@Test
	@DisplayName("guidance event enum exposes API contract values")
	void routeGuidanceEventTypeExposesContractValues() {
		assertThat(RouteGuidanceEventType.values())
			.extracting(Enum::name)
			.contains(
				"TURN_LEFT",
				"TURN_RIGHT",
				"CROSSWALK",
				"CROSSWALK_SIGNAL",
				"CROSSWALK_AUDIO",
				"STAIR",
				"NARROW_SIDEWALK",
				"UNPAVED",
				"LOW_SLOPE",
				"MIDDLE_SLOPE",
				"BUS_STOP",
				"SUBWAY_ELEVATOR",
				"ARRIVING_POINT",
				"DESTINATION")
			.doesNotContain("NONE", "CURB", "ELEVATOR", "ALIGHTING_POINT");
	}
}
