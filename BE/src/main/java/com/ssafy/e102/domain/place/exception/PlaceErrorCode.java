package com.ssafy.e102.domain.place.exception;

import org.springframework.http.HttpStatus;

import com.ssafy.e102.global.exception.ErrorCode;

public enum PlaceErrorCode implements ErrorCode {

	VOICE_ANALYSIS_AI_FAILED(HttpStatus.BAD_GATEWAY, "V5020", "음성 분석 AI 호출에 실패했습니다."),
	INVALID_PLACE_REQUEST(HttpStatus.BAD_REQUEST, "PL4000", "장소 조회 요청값이 올바르지 않습니다."),
	PLACE_KEYWORD_REQUIRED(HttpStatus.BAD_REQUEST, "PL4001", "장소 검색어를 입력해주세요."),
	PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PL4040", "장소를 찾을 수 없습니다."),
	PLACE_SEARCH_EXTERNAL_API_FAILED(HttpStatus.BAD_GATEWAY, "PL5020", "장소 검색 외부 API 호출에 실패했습니다.");

	private final HttpStatus httpStatus;
	private final String status;
	private final String message;

	PlaceErrorCode(HttpStatus httpStatus, String status, String message) {
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
