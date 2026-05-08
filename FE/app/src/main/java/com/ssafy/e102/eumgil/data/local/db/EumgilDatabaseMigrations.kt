package com.ssafy.e102.eumgil.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object EumgilDatabaseMigrations {
    val MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE reportDraft ADD COLUMN locationSource TEXT")
                database.execSQL("ALTER TABLE reportDraft ADD COLUMN photoMimeType TEXT")
                database.execSQL("ALTER TABLE reportDraft ADD COLUMN photoSizeBytes INTEGER")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reportOutbox (
                        outboxId TEXT NOT NULL PRIMARY KEY,
                        reportCategory TEXT NOT NULL,
                        description TEXT NOT NULL,
                        address TEXT,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        photoUri TEXT,
                        photoMimeType TEXT,
                        photoSizeBytes INTEGER,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_reportOutbox_status ON reportOutbox(status)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_reportOutbox_updatedAt ON reportOutbox(updatedAt)")
            }
        }

    val all: Array<Migration> = arrayOf(MIGRATION_1_2)
}
