package com.ssafy.e102.domain.route.dto.response;

/**
 * route guidance layer가 응답 step에 노출하는 alert type이다.
 *
 * <p>{@code TURN_LEFT}와 {@code TURN_RIGHT}는 원천 {@code segment_features} type이 아니라
 * geometry 기반으로 계산된 alert다.
 */
public enum RouteStepAlertType {
	NONE,
	TURN_LEFT,
	TURN_RIGHT
}
