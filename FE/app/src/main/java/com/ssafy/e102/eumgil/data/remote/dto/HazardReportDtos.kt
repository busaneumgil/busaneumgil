package com.ssafy.e102.eumgil.data.remote.dto

data class HazardReportPointDto(
    val lat: Double,
    val lng: Double,
)

data class CreateHazardReportRequestDto(
    val reportType: String,
    val description: String?,
    val reportPoint: HazardReportPointDto,
    val imageUrls: List<String> = emptyList(),
)

/**
 * `POST /hazard-reports/images/presigned-upload` 요청 (Task 5.5).
 *
 * BE는 이 요청을 받아 S3/MinIO에 PUT 가능한 presigned URL과 안정 저장값인 `objectKey`를 발급한다.
 * BE 명세: 모든 필드 필수. 허용 contentType은 image/jpeg, image/png, image/webp, image/heic, image/heif.
 * contentLength 기본 상한 10MB.
 */
data class PresignedUploadRequestDto(
    val fileName: String,
    val contentType: String,
    val contentLength: Long,
)

data class PresignedUploadResponseDto(
    val uploadUrl: String,
    val objectKey: String,
    val expiresAt: String,
)

data class CreateHazardReportResponseDto(
    val reportId: Long,
)

data class HazardReportListItemDto(
    val reportId: Long,
    val reportType: String,
    val reportPoint: HazardReportPointDto,
    val createdAt: String,
    val representativeImageUrl: String?,
)

data class HazardReportPageDto(
    val content: List<HazardReportListItemDto>,
    val size: Int,
    val nextCursor: Long?,
    val hasNext: Boolean,
)

data class HazardReportDetailDto(
    val reportId: Long,
    val reportType: String,
    val description: String?,
    val reportPoint: HazardReportPointDto,
    val createdAt: String,
    val imageUrls: List<String> = emptyList(),
)
