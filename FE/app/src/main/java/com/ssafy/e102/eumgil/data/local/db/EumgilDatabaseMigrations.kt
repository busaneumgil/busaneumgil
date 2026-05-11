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

    val MIGRATION_2_3: Migration =
        object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE favoriteRoute ADD COLUMN transportMode TEXT")
            }
        }

    val MIGRATION_3_4: Migration =
        object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE reportOutbox ADD COLUMN serverReportId INTEGER")
                database.execSQL("ALTER TABLE reportOutbox ADD COLUMN lastFailureReason TEXT")
            }
        }

    val MIGRATION_4_5: Migration =
        object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE bookmark ADD COLUMN serverBookmarkId INTEGER")
                database.execSQL("ALTER TABLE bookmark ADD COLUMN bookmarkTargetId TEXT")
                database.execSQL("ALTER TABLE bookmark ADD COLUMN targetType TEXT")
                database.execSQL("ALTER TABLE bookmark ADD COLUMN serverPlaceId INTEGER")
                database.execSQL("ALTER TABLE bookmark ADD COLUMN provider TEXT")
                database.execSQL("ALTER TABLE bookmark ADD COLUMN providerPlaceId TEXT")
                database.execSQL("ALTER TABLE bookmark ADD COLUMN providerCategory TEXT")
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_bookmark_bookmarkTargetId ON bookmark(bookmarkTargetId)",
                )
            }
        }

    val all: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
