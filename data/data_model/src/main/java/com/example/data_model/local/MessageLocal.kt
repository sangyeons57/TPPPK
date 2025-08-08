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

    // syncStatus 필드 제거 - OutBox에서만 동기화 상태 관리
)


@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: MessageEntity)

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

    /**
     * 지정된 시점 이전의 최근 메시지들을 ASC(오래된→최신)로 반환
     * - 내부 서브쿼리에서 DESC + LIMIT로 최근 N개를 뽑고, 바깥에서 ASC로 재정렬
     * - 채팅 초기 로딩 및 Prepend 시 DB 레벨에서부터 ASC 정렬된 결과를 사용하기 위함
     */
    @Query(
        """
        SELECT * FROM (
            SELECT * FROM messages
            WHERE (:channelId = '' OR channelId = :channelId) AND createdAt < :beforeTimestamp
            ORDER BY createdAt DESC
            LIMIT :limit
        ) AS sub
        ORDER BY createdAt ASC
        """
    )
    suspend fun getMessagesBeforeAsc(
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
        ORDER BY createdAt ASC
    """
    )
    suspend fun getMessagesBetween(
        channelId: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): List<MessageEntity>

    // ================================
    // 기본 조회 쿼리
    // ================================

    @Query(
        """
        SELECT * FROM messages 
        WHERE id = :messageId
    """
    )
    suspend fun findById(messageId: String): MessageEntity?

    @Query("UPDATE messages SET isDeleted = 1, updatedAt = :ts WHERE id = :id")
    suspend fun tombstone(id: String, ts: Long)

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
    suspend fun getRecentMessagesByChannel(channelId: String, limit: Int): List<MessageEntity>
}

/**
 * 채널별 메시지 개수를 담는 데이터 클래스
 */
data class ChannelMessageCount(
    val channelId: String,
    val count: Int
)