package com.example.data_datasource.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database Migration: Version 8 → 9
 * - Add tasks table
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create tasks table if not exists
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT NOT NULL PRIMARY KEY,
                channelId TEXT NOT NULL,
                taskType TEXT NOT NULL,
                status TEXT NOT NULL,
                content TEXT NOT NULL,
                `order` INTEGER NOT NULL,
                checkedBy TEXT,
                checkedAt INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Indices
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_tasks_channelId_order ON tasks(channelId, `order`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_tasks_channelId_updatedAt ON tasks(channelId, updatedAt)"
        )
    }
}

