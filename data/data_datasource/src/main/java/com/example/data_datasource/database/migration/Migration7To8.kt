package com.example.data_datasource.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database Migration: Version 7 → 8
 * - Add composite index on messages(channelId, createdAt) to improve query performance
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        try {
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_messages_channelId_createdAt ON messages(channelId, createdAt)"
            )
            android.util.Log.d("Migration", "✅ Created index index_messages_channelId_createdAt on messages(channelId, createdAt)")
        } catch (e: Exception) {
            android.util.Log.e("Migration", "❌ Failed to create index on messages: ${e.message}", e)
            // Do not throw to avoid breaking migration in case index already exists with a different name
        }
    }
}


