package com.ssafy.e102.domain.route.dto.response;

import java.math.BigDecimal;

/**
 * step에서 사용자에게 별도로 알려야 하는 alert 정보를 담는다.
 *
 * <p>CROSSWALK 같은 feature 기반 alert와 TURN_LEFT/TURN_RIGHT 같은 geometry 기반 alert가 모두 이 응답으로 노출된다.
 */
public record RouteStepAlertResponse(
	RouteStepAlertType type,
	BigDecimal distanceMeter) {
}
