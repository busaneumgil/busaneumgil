package com.ssafy.e102.domain.report.exception;

import org.springframework.http.HttpStatus;

import com.ssafy.e102.global.exception.ErrorCode;

public enum HazardReportErrorCode implements ErrorCode {

	INVALID_HAZARD_REPORT_REQUEST(HttpStatus.BAD_REQUEST, "HR4000", "제보 요청값이 올바르지 않습니다."),
	HAZARD_REPORT_FORBIDDEN(HttpStatus.FORBIDDEN, "HR4030", "제보에 대한 권한이 없습니다."),
	HAZARD_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "HR4040", "제보를 찾을 수 없습니다."),
	HAZARD_REPORT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "HR4090", "이미 처리된 제보입니다.");

	private final HttpStatus httpStatus;
	private final String status;
	private final String message;

	HazardReportErrorCode(HttpStatus httpStatus, String status, String message) {
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
