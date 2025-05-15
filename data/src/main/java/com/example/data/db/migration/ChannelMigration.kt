package com.example.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.domain.model.ChannelType

/**
 * 채널 테이블 구조 마이그레이션:
 * - 이전: metadata, participantIds, order 필드 사용
 * - 새로운: projectSpecificData, dmSpecificData 필드와 관련 정보 사용
 */
object ChannelMigration {

    /**
     * 채널 테이블을 새로운 구조로 마이그레이션 (버전 2 → 3)
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 1. 임시 테이블 생성
            database.execSQL(
                """
                CREATE TABLE channels_new (
                    id TEXT NOT NULL, 
                    name TEXT NOT NULL,
                    description TEXT,
                    type TEXT NOT NULL,
                    channelMode TEXT,
                    projectId TEXT,
                    categoryId TEXT,
                    channelOrder INTEGER NOT NULL DEFAULT 0,
                    participantIds TEXT NOT NULL DEFAULT '',
                    lastMessagePreview TEXT,
                    lastMessageTimestamp INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    createdBy TEXT,
                    cachedAt INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(id)
                )
                """
            )

            // 2. 인덱스 생성
            database.execSQL(
                "CREATE INDEX index_channels_new_projectId ON channels_new(projectId)"
            )

            // 3. 기존 데이터를 새 구조로 변환하여 복사
            // 참고: 기존 테이블에는 metadata, order, participantIds 필드가 있었음
            database.execSQL(
                """
                INSERT INTO channels_new (
                    id, name, type, 
                    projectId, categoryId, channelOrder,
                    participantIds, 
                    lastMessagePreview, lastMessageTimestamp,
                    cachedAt
                )
                SELECT 
                    id, name, type,
                    projectId, categoryId, "order",
                    participantIds,
                    lastMessagePreview, lastMessageTimestamp,
                    cachedAt
                FROM channels
                """
            )

            // 4. 날짜 필드 설정 (createdAt, updatedAt)
            database.execSQL(
                """
                UPDATE channels_new
                SET createdAt = cachedAt,
                    updatedAt = cachedAt
                """
            )

            // 5. channelMode 설정 (기본값 'TEXT')
            database.execSQL(
                """
                UPDATE channels_new
                SET channelMode = 'TEXT'
                """
            )

            // 6. 기존 테이블 삭제
            database.execSQL("DROP TABLE channels")

            // 7. 새 테이블 이름 변경
            database.execSQL("ALTER TABLE channels_new RENAME TO channels")
        }
    }
} 