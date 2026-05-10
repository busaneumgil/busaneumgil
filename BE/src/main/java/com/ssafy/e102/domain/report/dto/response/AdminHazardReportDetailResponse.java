package com.ssafy.e102.domain.report.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.ssafy.e102.domain.report.entity.HazardReport;
import com.ssafy.e102.domain.report.type.ReportStatus;
import com.ssafy.e102.domain.report.type.ReportType;
import com.ssafy.e102.global.geo.GeoPointConverter;
import com.ssafy.e102.global.geo.dto.GeoPointResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 제보 상세 응답")
public record AdminHazardReportDetailResponse(
	@Schema(description = "제보 ID", example = "1")
	Long reportId,
	@Schema(description = "제보자 사용자 ID", example = "7dafc215-b297-4f6c-bd7f-bc77fbb421a2")
	UUID reporterUserId,
	@Schema(description = "제보 유형", example = "SIDEWALK_MISSING")
	ReportType reportType,
	@Schema(description = "제보 설명", example = "보행 가능한 인도가 없습니다.")
	String description,
	@Schema(description = "제보 좌표")
	GeoPointResponse reportPoint,
	@Schema(description = "처리 상태", example = "PENDING")
	ReportStatus status,
	@Schema(description = "등록 일시", example = "2026-05-07T22:00:00")
	LocalDateTime createdAt,
	@Schema(description = "전체 첨부 이미지 URL 목록")
	List<String> imageUrls) {

	public static AdminHazardReportDetailResponse of(
		HazardReport hazardReport,
		GeoPointConverter geoPointConverter) {
		return new AdminHazardReportDetailResponse(
			hazardReport.getReportId(),
			hazardReport.getUser().getUserId(),
			hazardReport.getReportType(),
			hazardReport.getDescription(),
			geoPointConverter.toResponse(hazardReport.getReportPoint()),
			hazardReport.getStatus(),
			hazardReport.getCreatedAt(),
			hazardReport.getImages()
				.stream()
				.map(image -> image.getImageUrl())
				.toList());
	}
}
