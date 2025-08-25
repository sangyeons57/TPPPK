package com.example.data_datasource.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database Migration: Version 9 → 10
 * - Add deletedAt column to tasks table (soft delete support)
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add deletedAt column to tasks table for soft delete functionality
        database.execSQL("ALTER TABLE tasks ADD COLUMN deletedAt INTEGER")
    }
}