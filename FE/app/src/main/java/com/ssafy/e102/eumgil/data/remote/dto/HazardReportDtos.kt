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
