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
