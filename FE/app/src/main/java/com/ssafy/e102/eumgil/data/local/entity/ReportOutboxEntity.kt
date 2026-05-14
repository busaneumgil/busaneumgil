package com.ssafy.e102.eumgil.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reportOutbox",
    indices = [
        Index(value = ["status"]),
        Index(value = ["updatedAt"]),
    ],
)
data class ReportOutboxEntity(
    @PrimaryKey
    val outboxId: String,
    val reportCategory: String,
    val description: String = "",
    // v8 — 두 컬럼 분리: address는 좌표 → RGC 자동 결과, addressDetail은 사용자 직접 보충 메모.
    val address: String? = null,
    val addressDetail: String? = null,
    val latitude: Double,
    val longitude: Double,
    val photoUri: String? = null,
    val photoMimeType: String? = null,
    val photoSizeBytes: Long? = null,
    val status: String,
    val serverReportId: Long? = null,
    val lastFailureReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
