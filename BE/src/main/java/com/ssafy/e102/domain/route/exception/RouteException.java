package com.ssafy.e102.domain.route.exception;

import com.ssafy.e102.global.exception.BusinessException;

public class RouteException extends BusinessException {

	public RouteException(RouteErrorCode errorCode) {
		super(errorCode);
	}

	public RouteException(RouteErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public RouteException(RouteErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message, cause);
	}
}
