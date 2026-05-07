package com.ssafy.e102.domain.route.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ssafy.e102.domain.route.dto.request.WalkRouteSearchRequest;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class WalkRouteSearchRequestValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("도보 경로 검색 요청은 출발지와 도착지 좌표가 필수다")
	void walkRouteSearchRequestRequiresStartAndEndPoints() {
		WalkRouteSearchRequest request = new WalkRouteSearchRequest(null, null);

		Set<ConstraintViolation<WalkRouteSearchRequest>> violations = validator.validate(request);

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("startPoint", "endPoint");
	}

	@Test
	@DisplayName("도보 경로 검색 요청은 중첩 좌표 validation을 수행한다")
	void walkRouteSearchRequestValidatesNestedGeoPoints() {
		WalkRouteSearchRequest request = new WalkRouteSearchRequest(
			new GeoPointRequest(91.0, 128.936),
			new GeoPointRequest(35.1315, 181.0));

		Set<ConstraintViolation<WalkRouteSearchRequest>> violations = validator.validate(request);

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("startPoint.lat", "endPoint.lng");
	}
}
