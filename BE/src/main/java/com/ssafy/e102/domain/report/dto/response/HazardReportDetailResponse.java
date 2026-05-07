package com.ssafy.e102.domain.report.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.e102.domain.report.entity.HazardReport;
import com.ssafy.e102.domain.report.type.ReportType;
import com.ssafy.e102.global.geo.GeoPointConverter;
import com.ssafy.e102.global.geo.dto.GeoPointResponse;

public record HazardReportDetailResponse(
	Long reportId,
	ReportType reportType,
	String description,
	GeoPointResponse reportPoint,
	LocalDateTime createdAt,
	List<String> imageUrls) {

	public static HazardReportDetailResponse of(
		HazardReport hazardReport,
		GeoPointConverter geoPointConverter) {
		return new HazardReportDetailResponse(
			hazardReport.getReportId(),
			hazardReport.getReportType(),
			hazardReport.getDescription(),
			geoPointConverter.toResponse(hazardReport.getReportPoint()),
			hazardReport.getCreatedAt(),
			hazardReport.getImages()
				.stream()
				.map(image -> image.getImageUrl())
				.toList());
	}
}
