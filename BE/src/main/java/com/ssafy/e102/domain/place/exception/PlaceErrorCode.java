package com.ssafy.e102.domain.place.exception;

import org.springframework.http.HttpStatus;

import com.ssafy.e102.global.exception.ErrorCode;

public enum PlaceErrorCode implements ErrorCode {

	VOICE_ANALYSIS_AI_FAILED(HttpStatus.BAD_GATEWAY, "V5020", "음성 분석 AI 호출에 실패했습니다.");

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
