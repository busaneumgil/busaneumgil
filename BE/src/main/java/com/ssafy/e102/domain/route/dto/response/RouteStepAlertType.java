package com.ssafy.e102.domain.route.dto.response;

/**
 * route step 응답에 노출할 alert 유형이다.
 *
 * <p>회전 방향은 alert가 아니라 {@code RouteStepResponse.instruction} 문구로 노출한다.
 */
public enum RouteStepAlertType {
	CROSSWALK,
	CROSSWALK_SIGNAL,
	CROSSWALK_AUDIO,
	MIDDLE_SLOPE,
	STAIR,
	NARROW_SIDEWALK,
	UNPAVED,
	ELEVATOR,
	BUS_STOP,
	SUBWAY_ELEVATOR,
	ALIGHTING_POINT
}
