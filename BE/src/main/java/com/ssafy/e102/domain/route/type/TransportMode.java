package com.ssafy.e102.domain.route.type;

/**
 * route와 leg가 사용하는 이동 수단 타입이다.
 *
 * <p>이번 도보 검색 브랜치는 WALK만 열고, transit 브랜치에서 BUS/SUBWAY 확장을 다룬다.
 */
public enum TransportMode {
	WALK
}
