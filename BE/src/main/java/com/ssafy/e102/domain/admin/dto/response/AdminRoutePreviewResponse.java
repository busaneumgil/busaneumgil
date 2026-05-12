package com.ssafy.e102.domain.admin.dto.response;

public record AdminRoutePreviewResponse(
	AdminRoutePreviewItemResponse baseRoute,
	AdminRoutePreviewItemResponse tunedRoute) {
}
