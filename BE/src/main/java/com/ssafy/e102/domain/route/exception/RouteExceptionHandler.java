package com.ssafy.e102.domain.route.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ssafy.e102.domain.route.controller.RouteController;
import com.ssafy.e102.global.response.ErrorResponse;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = RouteController.class)
public class RouteExceptionHandler {

	@ExceptionHandler({
		MethodArgumentNotValidException.class,
		HttpMessageNotReadableException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRouteRequest(Exception exception) {
		return ResponseEntity
			.status(RouteErrorCode.INVALID_ROUTE_REQUEST.getHttpStatus())
			.body(ErrorResponse.from(RouteErrorCode.INVALID_ROUTE_REQUEST));
	}
}
