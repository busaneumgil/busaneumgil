package com.ssafy.e102.eumgil.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reportDraft",
    indices = [Index(value = ["updatedAt"])],
)
data class ReportDraftEntity(
    @PrimaryKey
    val draftId: String,
    val reportCategory: String? = null,
    val description: String = "",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
