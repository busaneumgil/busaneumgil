package com.ssafy.e102.domain.admin.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 보행 네트워크 조회 응답")
public record AdminRoadNetworkResponse(
	@Schema(description = "요약 정보")
	AdminRoadNetworkSummaryResponse summary,
	@Schema(description = "조회 결과 bbox. [minLng, minLat, maxLng, maxLat]")
	List<Double> bbox,
	@Schema(description = "보행 네트워크 segment GeoJSON")
	AdminGeoJsonFeatureCollectionResponse<AdminGeoJsonFeatureResponse<AdminLineStringGeometryResponse, AdminRoadSegmentPropertiesResponse>> segments,
	@Schema(description = "보행 네트워크 node GeoJSON")
	AdminGeoJsonFeatureCollectionResponse<AdminGeoJsonFeatureResponse<AdminPointGeometryResponse, AdminRoadNodePropertiesResponse>> roadNodes) {
}
