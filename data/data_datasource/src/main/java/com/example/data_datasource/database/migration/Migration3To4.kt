package com.example.data_datasource.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database Migration: Version 3 → 4
 * messages 테이블에 channel_id 컬럼 추가
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // messages 테이블에 channel_id 컬럼 추가
        database.execSQL(
            "ALTER TABLE messages ADD COLUMN channel_id TEXT NOT NULL DEFAULT ''"
        )

        // 기존 데이터의 경우 빈 문자열로 초기화
        // 실제 운영에서는 기존 데이터를 적절히 마이그레이션해야 함
        database.execSQL(
            "UPDATE messages SET channel_id = '' WHERE channel_id IS NULL"
        )
    }
} 