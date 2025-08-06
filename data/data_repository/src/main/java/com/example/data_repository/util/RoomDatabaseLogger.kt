package com.example.data_repository.util

import android.database.Cursor
import android.util.Log
import com.example.data_datasource.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room 데이터베이스 상태를 예쁘게 출력하는 유틸리티
 * DI Singleton으로 관리되며 AppDatabase를 주입받습니다.
 */
@Singleton
class RoomDatabaseLogger @Inject constructor(
    private val appDatabase: AppDatabase
) {

    // 로그 태그
    private val TAG = "RoomDB"

    // 테이블명 상수 정의
    companion object {
        const val TABLE_MESSAGES = "messages"
        const val TABLE_OUTBOX_RECORD = "outboxRecord"
        const val TABLE_SYNC_METADATA = "syncMetadata"
    }

    // 테이블 정보 정의 (상수 사용)
    private val TABLE_INFO = mapOf(
        TABLE_MESSAGES to TableInfo(
            name = TABLE_MESSAGES,
            description = "채팅 메시지",
            sampleFields = listOf(
                "id",
                "channelId",
                "senderId",
                "content",
                "createdAt",
                "syncStatus"
            )
        ),
        TABLE_OUTBOX_RECORD to TableInfo(
            name = TABLE_OUTBOX_RECORD,
            description = "동기화 대기열",
            sampleFields = listOf("id", "stream", "aggregateId", "op", "createdAt")
        ),
        TABLE_SYNC_METADATA to TableInfo(
            name = TABLE_SYNC_METADATA,
            description = "동기화 메타데이터",
            sampleFields = listOf("stream", "lastCursor", "lastSyncAt")
        )
    )

    /**
     * 전체 데이터베이스 상태를 로그로 출력
     */
    suspend fun logDatabaseState() {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "=".repeat(80))
            Log.i(TAG, "📊 ROOM DATABASE STATE REPORT")
            Log.i(TAG, "=".repeat(80))

            TABLE_INFO.forEach { (tableName, tableInfo) ->
                logTableStateInternal(tableName, tableInfo)
            }

            Log.i(TAG, "=".repeat(80))
            Log.i(TAG, "📊 END OF DATABASE REPORT")
            Log.i(TAG, "=".repeat(80))
        }
    }

    /**
     * 특정 테이블 상태를 로그로 출력 (내부 사용, 이미 IO 스레드에서 실행됨)
     */
    private fun logTableStateInternal(tableName: String, tableInfo: TableInfo) {
        try {
            val count = getTableCount(tableName)
            val sampleData = getSampleData(tableName, 3)

            Log.i(TAG, "")
            Log.i(TAG, "📋 TABLE: ${tableInfo.name}")
            Log.i(TAG, "📝 Description: ${tableInfo.description}")
            Log.i(TAG, "📊 Record Count: $count")

            if (count > 0) {
                Log.i(TAG, "📄 Sample Data:")
                sampleData.forEachIndexed { index, row ->
                    Log.i(TAG, "   ${index + 1}. $row")
                }
            } else {
                Log.i(TAG, "   (empty table)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to log table state for $tableName: ${e.message}")
        }
    }

    /**
     * 테이블 레코드 수 조회
     */
    private fun getTableCount(tableName: String): Int {
        return try {
            val cursor = appDatabase.query("SELECT COUNT(*) FROM $tableName", emptyArray())
            val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()
            count
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get table count for $tableName: ${e.message}")
            0
        }
    }

    /**
     * 샘플 데이터 조회 (최대 limit개) - 테이블별 최적화된 쿼리 사용
     */
    private fun getSampleData(tableName: String, limit: Int): List<String> {
        return try {
            // 테이블별로 최적화된 쿼리 사용
            val query = when (tableName) {
                TABLE_MESSAGES -> "SELECT id, channelId, senderId, substr(payload, 1, 30) as content_preview, createdAt, syncStatus FROM messages ORDER BY createdAt DESC LIMIT $limit"
                TABLE_OUTBOX_RECORD -> "SELECT id, stream, aggregateId, op, createdAt FROM outboxRecord ORDER BY createdAt DESC LIMIT $limit"
                TABLE_SYNC_METADATA -> "SELECT * FROM sync_metadata ORDER BY lastSyncAt DESC LIMIT $limit"
                else -> "SELECT * FROM $tableName ORDER BY ROWID DESC LIMIT $limit"
            }

            val cursor = appDatabase.query(query, emptyArray())
            val results = mutableListOf<String>()
            val columnNames = cursor.columnNames

            while (cursor.moveToNext()) {
                val rowData = columnNames.mapIndexed { index, columnName ->
                    val value = when (cursor.getType(index)) {
                        Cursor.FIELD_TYPE_NULL -> "NULL"
                        Cursor.FIELD_TYPE_INTEGER -> {
                            val longValue = cursor.getLong(index)
                            // 타임스탬프인 경우 포맷팅
                            if (columnName.contains(
                                    "At",
                                    ignoreCase = true
                                ) && longValue > 1000000000000L
                            ) {
                                "${longValue} (${formatTimestamp(longValue)})"
                            } else {
                                longValue.toString()
                            }
                        }

                        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index).toString()
                        Cursor.FIELD_TYPE_STRING -> {
                            val stringValue = cursor.getString(index) ?: "NULL"
                            // 긴 문자열은 줄임표 처리
                            if (stringValue.length > 50) {
                                "\"${stringValue.take(47)}...\""
                            } else {
                                "\"$stringValue\""
                            }
                        }

                        Cursor.FIELD_TYPE_BLOB -> "[BLOB]"
                        else -> "UNKNOWN"
                    }
                    "$columnName=$value"
                }.joinToString(", ")

                results.add("{$rowData}")
            }

            cursor.close()
            results
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get sample data for $tableName: ${e.message}")
            emptyList()
        }
    }

    /**
     * 타임스탬프를 읽기 쉬운 형태로 포맷팅
     */
    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val dateFormat =
                java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault())
            dateFormat.format(java.util.Date(timestamp))
        } catch (e: Exception) {
            "Invalid"
        }
    }

    /**
     * 테이블 스키마 정보 출력
     */
    suspend fun logTableSchema(tableName: String) {
        withContext(Dispatchers.IO) {
            try {
                val cursor = appDatabase.query("PRAGMA table_info($tableName)", emptyArray())

                Log.i(TAG, "")
                Log.i(TAG, "🏗️ SCHEMA: $tableName")
                Log.i(TAG, "┌" + "─".repeat(60) + "┐")
                Log.i(
                    TAG,
                    "│ ${"Column".padEnd(20)} │ ${"Type".padEnd(15)} │ ${"Nullable".padEnd(8)} │ ${
                        "Primary Key".padEnd(12)
                    } │"
                )
                Log.i(TAG, "├" + "─".repeat(60) + "┤")

                while (cursor.moveToNext()) {
                    val columnName = cursor.getString(1)
                    val dataType = cursor.getString(2)
                    val notNull = if (cursor.getInt(3) == 1) "NO" else "YES"
                    val primaryKey = if (cursor.getInt(5) == 1) "YES" else "NO"

                    Log.i(
                        TAG,
                        "│ ${columnName.padEnd(20)} │ ${dataType.padEnd(15)} │ ${notNull.padEnd(8)} │ ${
                            primaryKey.padEnd(12)
                        } │"
                    )
                }

                Log.i(TAG, "└" + "─".repeat(60) + "┘")
                cursor.close()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to log schema for $tableName: ${e.message}")
            }
        }
    }

    /**
     * 특정 테이블의 상세 정보 출력 (스키마 + 샘플 데이터)
     */
    suspend fun logTableDetails(tableName: String) {
        withContext(Dispatchers.IO) {
            val tableInfo = TABLE_INFO[tableName]
            if (tableInfo != null) {
                logTableStateInternal(tableName, tableInfo)
                logTableSchema(tableName)
            } else {
                Log.w(TAG, "⚠️ Unknown table: $tableName")
            }
        }
    }

    /**
     * 특정 테이블 상태를 로그로 출력 (외부에서 호출 가능)
     */
    suspend fun logTableState(tableName: String) {
        withContext(Dispatchers.IO) {
            val tableInfo = TABLE_INFO[tableName]
            if (tableInfo != null) {
                logTableStateInternal(tableName, tableInfo)
            } else {
                Log.w(TAG, "⚠️ Unknown table: $tableName")
            }
        }
    }

    /**
     * 특정 채널의 메시지 상태를 상세히 로그로 출력
     */
    suspend fun logChannelMessages(channelId: String, limit: Int = 10) {
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "")
                Log.i(TAG, "💬 === CHANNEL MESSAGES: $channelId ===")

                // 채널별 메시지 개수
                val countCursor = appDatabase.query(
                    "SELECT COUNT(*) FROM messages WHERE channelId = ?",
                    arrayOf(channelId)
                )
                val totalCount = if (countCursor.moveToFirst()) countCursor.getInt(0) else 0
                countCursor.close()

                Log.i(TAG, "📊 Total messages in channel: $totalCount")

                if (totalCount > 0) {
                    // 동기화 상태별 개수
                    val statusCursor = appDatabase.query(
                        "SELECT syncStatus, COUNT(*) as count FROM messages WHERE channelId = ? GROUP BY syncStatus",
                        arrayOf(channelId)
                    )
                    Log.i(TAG, "📈 Messages by sync status:")
                    while (statusCursor.moveToNext()) {
                        val status = statusCursor.getString(0) ?: "NULL"
                        val count = statusCursor.getInt(1)
                        Log.i(TAG, "   $status: $count messages")
                    }
                    statusCursor.close()

                    // 최신 메시지들
                    val messagesCursor = appDatabase.query(
                        "SELECT id, senderId, substr(payload, 1, 40) as content_preview, createdAt, syncStatus FROM messages WHERE channelId = ? ORDER BY createdAt DESC LIMIT ?",
                        arrayOf(channelId, limit.toString())
                    )

                    Log.i(TAG, "📝 Recent messages (최신 ${limit}개):")
                    var index = 1
                    while (messagesCursor.moveToNext()) {
                        val id = messagesCursor.getString(0)?.take(8) ?: "unknown"
                        val senderId = messagesCursor.getString(1)?.take(8) ?: "unknown"
                        val content = messagesCursor.getString(2) ?: ""
                        val createdAt = messagesCursor.getLong(3)
                        val syncStatus = messagesCursor.getString(4) ?: ""

                        val timeFormatted = formatTimestamp(createdAt)
                        Log.i(
                            TAG,
                            "   $index. [$id] $senderId ($syncStatus) $timeFormatted: \"$content\""
                        )
                        index++
                    }
                    messagesCursor.close()
                } else {
                    Log.i(TAG, "   📭 No messages found in this channel")
                }

                Log.i(TAG, "💬 === END CHANNEL MESSAGES ===")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to log channel messages for $channelId: ${e.message}")
            }
        }
    }
}

/**
 * 테이블 정보를 담는 데이터 클래스
 */
internal data class TableInfo(
    val name: String,
    val description: String,
    val sampleFields: List<String>
)

 