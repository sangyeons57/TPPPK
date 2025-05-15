package com.example.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 데이터베이스 마이그레이션을 집중적으로 관리하는 클래스
 * 
 * 이 클래스는 데이터베이스 버전 간의 마이그레이션을 정의하고 관리합니다.
 * 앱의 데이터베이스 스키마가 변경될 때마다 여기에 적절한 마이그레이션을 추가해야 합니다.
 */
object AppDatabaseMigrations {
    
    /**
     * 버전 6에서 7로의 마이그레이션
     * 
     * 변경 사항:
     * - InviteEntity에 inviterName 필드 추가
     * - InviteEntity에 projectName 필드 추가
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // InviteEntity 테이블에 새 필드 추가
            database.execSQL("ALTER TABLE invites ADD COLUMN inviterName TEXT NOT NULL DEFAULT '알 수 없음'")
            database.execSQL("ALTER TABLE invites ADD COLUMN projectName TEXT NOT NULL DEFAULT '알 수 없음'")
        }
    }
    
    /**
     * 버전 7에서 8로의 마이그레이션
     * 
     * 변경 사항:
     * - ChannelEntity 구조 변경
     * - metadata, order, participantIds 필드 제거
     * - projectSpecificData와 dmSpecificData 관련 필드 추가
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 채널 테이블 마이그레이션은 ChannelMigration.MIGRATION_2_3에 구현되어 있음
            ChannelMigration.MIGRATION_2_3.migrate(database)
        }
    }
    
    /**
     * 앱에서 사용할 모든 마이그레이션 배열
     * 새로운 마이그레이션을 추가할 때 이 배열에도 추가해야 합니다.
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_6_7,
        MIGRATION_7_8
        // 추후 추가될 마이그레이션들...
    )
} 