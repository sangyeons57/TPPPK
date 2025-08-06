package com.example.data_model.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "channelId")
    val channelId: String, // ✅ 채널 ID 추가

    @ColumnInfo(name = "senderId")
    val senderId: String,

    @ColumnInfo(name = "messageType", defaultValue = "TEXT")
    val messageType: String = "TEXT", // 메시지 타입 (TEXT, SYSTEM_PROJECT_JOIN, etc.)

    @ColumnInfo(name = "payload", defaultValue = "{}")
    val payload: String = "{}", // JSON 페이로드 (기존 content 대체)

    @ColumnInfo(name = "replyToMessageId")
    val replyToMessageId: String? = null,

    @ColumnInfo(name = "isDeleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "mentions", defaultValue = "[]")
    val mentions: String = "[]", // JSON string of mentions

    @ColumnInfo(name = "createdAt")
    val createdAt: Long, // Epoch milliseconds

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long, // Epoch milliseconds

    // Sync metadata fields
    @ColumnInfo(name = "syncStatus")
    val syncStatus: String = "",
)


@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: MessageEntity)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun findById(id: String): MessageEntity?

    @Query("UPDATE messages SET isDeleted = 1, updatedAt = :ts WHERE id = :id")
    suspend fun tombstone(id: String, ts: Long)

    // ================================
    // Paging3 관련 쿼리
    // ================================

    @Query(
        """
        SELECT * FROM messages 
        WHERE (:channelId = '' OR channelId = :channelId) AND createdAt < :beforeTimestamp 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """
    )
    suspend fun getMessagesBefore(
        channelId: String,
        beforeTimestamp: Long,
        limit: Int
    ): List<MessageEntity>

    @Query(
        """
        SELECT * FROM messages 
        WHERE (:channelId = '' OR channelId = :channelId) AND createdAt > :afterTimestamp 
        ORDER BY createdAt ASC 
        LIMIT :limit
    """
    )
    suspend fun getMessagesAfter(
        channelId: String,
        afterTimestamp: Long,
        limit: Int
    ): List<MessageEntity>

    @Query(
        """
        SELECT * FROM messages 
        WHERE (:channelId = '' OR channelId = :channelId) AND createdAt BETWEEN :startTimestamp AND :endTimestamp 
        ORDER BY createdAt DESC
    """
    )
    suspend fun getMessagesBetween(
        channelId: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): List<MessageEntity>

    // ================================
    // Anchor 기반 쿼리 (3-way 분할 지원)
    // ================================

    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId AND id = :anchorMessageId
    """
    )
    suspend fun getAnchorMessage(channelId: String, anchorMessageId: String): MessageEntity?

    @Query(
        """
        WITH anchor_time AS (
            SELECT createdAt FROM messages 
            WHERE channelId = :channelId AND id = :anchorMessageId
        )
        SELECT * FROM messages 
        WHERE channelId = :channelId 
        AND createdAt >= (SELECT createdAt FROM anchor_time) - :beforeRange
        AND createdAt <= (SELECT createdAt FROM anchor_time) + :afterRange
        ORDER BY createdAt DESC
        LIMIT :limit
    """
    )
    suspend fun getMessagesAroundAnchor(
        channelId: String,
        anchorMessageId: String,
        beforeRange: Long = 3600000, // 1시간 전
        afterRange: Long = 3600000,  // 1시간 후
        limit: Int = 50
    ): List<MessageEntity>

    // ================================
    // 동기화 상태 관련 쿼리
    // ================================

    @Query("UPDATE messages SET syncStatus = :syncStatus WHERE id = :messageId")
    suspend fun updateSyncStatus(messageId: String, syncStatus: String)

    @Query("UPDATE messages SET syncStatus = :syncStatus WHERE id IN (:messageIds)")
    suspend fun updateSyncStatusByIds(messageIds: List<String>, syncStatus: String): Int

    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId AND syncStatus = :syncStatus 
        ORDER BY createdAt ASC 
        LIMIT :limit
    """
    )
    suspend fun getMessagesBySyncStatus(
        channelId: String,
        syncStatus: String,
        limit: Int
    ): List<MessageEntity>

    @Query(
        """
        SELECT COUNT(*) FROM messages 
        WHERE channelId = :channelId AND syncStatus = :syncStatus
    """
    )
    suspend fun countMessagesBySyncStatus(channelId: String, syncStatus: String): Int

    // ================================
    // 캐시 관리 관련 쿼리
    // ================================

    /**
     * 특정 채널의 모든 메시지 삭제 (캐시 클리어용)
     */
    @Query("DELETE FROM messages WHERE channelId = :channelId")
    suspend fun deleteAllByChannelId(channelId: String): Int

    /**
     * 모든 메시지 삭제 (전체 캐시 클리어용)
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAll(): Int

    // ================================
    // 디버그 및 로깅용 쿼리
    // ================================

    /**
     * 특정 채널의 메시지 개수 조회
     */
    @Query("SELECT COUNT(*) FROM messages WHERE channelId = :channelId")
    suspend fun getMessageCountByChannel(channelId: String): Int

    /**
     * 특정 채널의 최신 메시지들 조회 (디버그용)
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """
    )
    suspend fun getRecentMessagesForDebug(channelId: String, limit: Int = 10): List<MessageEntity>

    /**
     * 동기화 상태별 메시지 개수 조회 (디버그용)
     */
    @Query(
        """
        SELECT syncStatus, COUNT(*) as count 
        FROM messages 
        WHERE channelId = :channelId 
        GROUP BY syncStatus
    """
    )
    suspend fun getSyncStatusCountsByChannel(channelId: String): List<SyncStatusCount>

    /**
     * 전체 메시지 개수 조회
     */
    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getTotalMessageCount(): Int

    /**
     * 채널별 메시지 개수 조회 (모든 채널)
     */
    @Query(
        """
        SELECT channelId, COUNT(*) as count 
        FROM messages 
        GROUP BY channelId 
        ORDER BY count DESC
    """
    )
    suspend fun getMessageCountsByChannel(): List<ChannelMessageCount>
}

/**
 * 동기화 상태별 개수를 담는 데이터 클래스
 */
data class SyncStatusCount(
    val syncStatus: String,
    val count: Int
)

/**
 * 채널별 메시지 개수를 담는 데이터 클래스
 */
data class ChannelMessageCount(
    val channelId: String,
    val count: Int
)