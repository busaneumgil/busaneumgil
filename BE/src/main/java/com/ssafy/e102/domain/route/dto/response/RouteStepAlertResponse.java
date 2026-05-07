package com.ssafy.e102.domain.route.dto.response;

import java.math.BigDecimal;

/**
 * step에서 사용자에게 별도로 알려야 하는 alert 정보를 담는다.
 *
 * <p>CROSSWALK 같은 주의 이벤트만 이 응답으로 노출한다. 좌/우회전 같은 방향 안내는 step instruction으로 노출한다.
 */
public record RouteStepAlertResponse(
	RouteStepAlertType type,
	BigDecimal distanceMeter) {
}
