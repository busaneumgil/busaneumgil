package com.ssafy.e102.global.response;

public record ApiResponse<T>(
	String status,
	T data,
	String message) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>("S2000", data, "정상 처리되었습니다.");
	}

	public static <T> ApiResponse<T> created(T data) {
		return new ApiResponse<>("S2010", data, "생성되었습니다.");
	}

	public static ApiResponse<Void> success() {
		return new ApiResponse<>("S2000", null, "정상 처리되었습니다.");
	}
}
