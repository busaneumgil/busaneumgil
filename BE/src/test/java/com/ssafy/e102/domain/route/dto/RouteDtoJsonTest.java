package com.ssafy.e102.domain.route.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteStepAlertResponse;
import com.ssafy.e102.domain.route.dto.response.RouteStepAlertType;
import com.ssafy.e102.domain.route.dto.response.RouteStepResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;

class RouteDtoJsonTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("도보 경로 검색 응답 DTO는 경로 API 계약 필드명과 enum 값을 직렬화한다")
	void walkRouteSearchResponseSerializesApiContract() throws Exception {
		WalkRouteSearchResponse response = new WalkRouteSearchResponse(
			"rs_walk_20260506_abc123",
			List.of(new RouteSummaryResponse(
				"walk_rt_safe_001",
				TransportMode.WALK,
				RouteOption.SAFE,
				"안전 경로",
				BigDecimal.valueOf(950),
				960,
				16,
				List.of(RouteBadge.LOW_SLOPE, RouteBadge.CROSSWALK),
				"LINESTRING(128.9360 35.1200, 128.8823 35.1315)",
				List.of(new RouteLegResponse(
					1,
					TransportMode.WALK,
					RouteLegRole.WALK_ONLY,
					"목적지까지 도보로 이동하세요.",
					BigDecimal.valueOf(950),
					960,
					16,
					"LINESTRING(128.9360 35.1200, 128.8823 35.1315)",
					List.of(new RouteStepResponse(
						1,
						"직진하세요.",
						BigDecimal.valueOf(30),
						35,
						"LINESTRING(128.9360 35.1200, 128.9361 35.1201)",
						new RouteStepAlertResponse(RouteStepAlertType.CROSSWALK_AUDIO, BigDecimal.valueOf(12)))))))));

		JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(response));

		assertThat(root.get("searchId").asText()).isEqualTo("rs_walk_20260506_abc123");
		JsonNode route = root.get("routes").get(0);
		assertThat(route.get("routeId").asText()).isEqualTo("walk_rt_safe_001");
		assertThat(route.get("transportMode").asText()).isEqualTo("WALK");
		assertThat(route.get("routeOption").asText()).isEqualTo("SAFE");
		assertThat(route.get("badges").get(0).asText()).isEqualTo("LOW_SLOPE");
		JsonNode leg = route.get("legs").get(0);
		assertThat(leg.get("type").asText()).isEqualTo("WALK");
		assertThat(leg.get("role").asText()).isEqualTo("WALK_ONLY");
		JsonNode step = leg.get("steps").get(0);
		assertThat(step.get("instruction").asText()).isEqualTo("직진하세요.");
		assertThat(step.get("alert").get("type").asText()).isEqualTo("CROSSWALK_AUDIO");
		assertThat(step.has("badges")).isFalse();
		assertThat(step.has("slopePercent")).isFalse();
		assertThat(step.has("widthState")).isFalse();
	}

	@Test
	@DisplayName("route step alert enum은 API 계약에 없는 NONE/CURB/TURN_LEFT/TURN_RIGHT를 노출하지 않는다")
	void routeStepAlertTypeDoesNotExposeRemovedContractValues() {
		assertThat(RouteStepAlertType.values())
			.extracting(Enum::name)
			.contains("CROSSWALK", "CROSSWALK_SIGNAL", "CROSSWALK_AUDIO", "STAIR", "NARROW_SIDEWALK", "UNPAVED",
				"MIDDLE_SLOPE")
			.doesNotContain("NONE", "CURB", "TURN_LEFT", "TURN_RIGHT");
	}
}
