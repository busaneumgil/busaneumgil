package com.ssafy.e102.domain.route.dto.response;

/**
 * WALK leg 위에서 FE가 알림 카드와 TTS로 소비할 안내 이벤트 유형이다.
 *
 * <p>방향 전환과 접근성 주의 지점을 같은 시간축에서 소비할 수 있게 하나의 enum으로 제공한다.
 */
public enum RouteGuidanceEventType {
	TURN_LEFT,
	TURN_RIGHT,
	CROSSWALK,
	CROSSWALK_SIGNAL,
	CROSSWALK_AUDIO,
	LOW_SLOPE,
	MIDDLE_SLOPE,
	STAIR,
	NARROW_SIDEWALK,
	UNPAVED,
	BUS_STOP,
	SUBWAY_ELEVATOR,
	ARRIVING_POINT,
	DESTINATION
}
