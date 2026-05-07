package com.ssafy.e102.domain.place.dto.response;

import java.util.List;

public record PlaceSearchResponse(
	List<PlaceSearchItemResponse> places,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext) {
}
