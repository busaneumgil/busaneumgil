package com.ssafy.e102.global.external.kakao;

public record KakaoPlaceSearchRequest(
	String keyword,
	Double lat,
	Double lng,
	Integer radius,
	int page,
	int size,
	String sort) {

	public KakaoPlaceSearchRequest(
		String keyword,
		Double lat,
		Double lng,
		Integer radius,
		int page,
		int size) {
		this(keyword, lat, lng, radius, page, size, null);
	}
}
