package com.ssafy.e102.global.external.kakao;

public record KakaoPlaceSearchRequest(
	String keyword,
	Double lat,
	Double lng,
	Integer radius,
	int page,
	int size) {
}
