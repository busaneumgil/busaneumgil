package com.ssafy.e102.domain.report.dto.response;

import java.time.LocalDateTime;

import com.ssafy.e102.domain.report.entity.HazardReport;
import com.ssafy.e102.domain.report.type.ReportType;
import com.ssafy.e102.global.geo.GeoPointConverter;
import com.ssafy.e102.global.geo.dto.GeoPointResponse;

public record HazardReportSummaryResponse(
	Long reportId,
	ReportType reportType,
	GeoPointResponse reportPoint,
	LocalDateTime createdAt,
	String representativeImageUrl) {

	public static HazardReportSummaryResponse of(
		HazardReport hazardReport,
		String representativeImageUrl,
		GeoPointConverter geoPointConverter) {
		return new HazardReportSummaryResponse(
			hazardReport.getReportId(),
			hazardReport.getReportType(),
			geoPointConverter.toResponse(hazardReport.getReportPoint()),
			hazardReport.getCreatedAt(),
			representativeImageUrl);
	}
}
