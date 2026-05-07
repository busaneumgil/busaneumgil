package com.ssafy.e102.domain.route.dto.response;

/**
 * route step 응답에 노출할 alert 유형이다.
 *
 * <p>{@code CROSSWALK}는 segment feature 기반으로, {@code TURN_LEFT}와 {@code TURN_RIGHT}는 route geometry 기반으로
 * 계산한다.
 */
public enum RouteStepAlertType {
	NONE,
	CROSSWALK,
	MIDDLE_SLOPE,
	STAIR,
	CURB,
	NARROW_SIDEWALK,
	UNPAVED,
	ELEVATOR,
	BUS_STOP,
	SUBWAY_ELEVATOR,
	ALIGHTING_POINT,
	TURN_LEFT,
	TURN_RIGHT
}
