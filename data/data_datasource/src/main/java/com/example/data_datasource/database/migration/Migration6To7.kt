package com.example.data_datasource.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database Migration: Version 6 → 7
 * outbox 테이블을 outboxRecord로 변경하는 마이그레이션
 * 이는 이전 버전에서 outbox 테이블을 사용했던 경우를 처리
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // outbox 테이블이 존재하면 outboxRecord로 이름 변경
        try {
            database.execSQL(
                "ALTER TABLE outbox RENAME TO outboxRecord"
            )
            android.util.Log.d("Migration", "outbox 테이블을 outboxRecord로 성공적으로 변경")
        } catch (e: Exception) {
            // outbox 테이블이 존재하지 않는 경우 무시 (새로운 설치)
            // 이는 정상적인 상황이므로 로그만 출력
            android.util.Log.d("Migration", "outbox 테이블이 존재하지 않음 - 새로운 설치")
        }
    }
}
