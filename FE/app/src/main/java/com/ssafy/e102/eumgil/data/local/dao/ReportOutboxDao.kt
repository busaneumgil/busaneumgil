package com.ssafy.e102.eumgil.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ssafy.e102.eumgil.data.local.entity.ReportOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportOutboxDao {
    @Query("SELECT * FROM reportOutbox ORDER BY updatedAt DESC")
    fun observeReportOutboxItems(): Flow<List<ReportOutboxEntity>>

    @Query("SELECT * FROM reportOutbox WHERE outboxId = :outboxId LIMIT 1")
    suspend fun getReportOutbox(outboxId: String): ReportOutboxEntity?

    @Upsert
    suspend fun upsertReportOutbox(reportOutbox: ReportOutboxEntity)

    @Query("DELETE FROM reportOutbox WHERE outboxId = :outboxId")
    suspend fun deleteReportOutbox(outboxId: String)
}
