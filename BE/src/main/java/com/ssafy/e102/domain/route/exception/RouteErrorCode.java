package com.ssafy.e102.domain.route.exception;

import org.springframework.http.HttpStatus;

import com.ssafy.e102.global.exception.ErrorCode;

/**
 * route 도메인에서 API 명세와 맞춰 반환하는 에러 코드다.
 *
 * <p>service와 external client에서 발생한 실패를 GlobalExceptionHandler가 공통 에러 응답으로 변환할 때 사용한다.
 */
public enum RouteErrorCode implements ErrorCode {

	INVALID_ROUTE_REQUEST(HttpStatus.BAD_REQUEST, "RT4000", "경로 요청값이 올바르지 않습니다."),
	OUT_OF_SERVICE_AREA(HttpStatus.BAD_REQUEST, "RT4003", "부산광역시 안의 위치를 선택해 주세요."),
	START_END_TOO_CLOSE(HttpStatus.BAD_REQUEST, "RT4004", "출발지와 도착지를 다르게 선택해 주세요."),
	ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "RT4040", "탐색 가능한 경로가 없습니다."),
	EXTERNAL_ROUTE_API_FAILED(HttpStatus.BAD_GATEWAY, "EX5020", "외부 경로 정보를 불러오지 못했습니다."),
	EXTERNAL_ROUTE_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "EX5040", "외부 경로 정보 응답이 지연되고 있습니다.");

	private final HttpStatus httpStatus;
	private final String status;
	private final String message;

	RouteErrorCode(HttpStatus httpStatus, String status, String message) {
		this.httpStatus = httpStatus;
		this.status = status;
		this.message = message;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
