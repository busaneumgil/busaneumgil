package com.ssafy.e102.eumgil.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object EumgilDatabaseMigrations {
    val MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Initial Room schema starts at version 1.
                // Fill in ALTER TABLE / backfill statements here when version 2 is introduced.
            }
        }

    val all: Array<Migration> = arrayOf(MIGRATION_1_2)
}
