package com.example.data_model.local

import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(
    tableName = "messages",
    indices = [Index(value = ["channelId", "createdAt"])]
)
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

)


@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: MessageEntity)

    /**
     * Room DAO에서 직접 PagingSource를 반환하는 정석 패턴
     * Room이 자동으로 무효화(invalidation)를 처리합니다.
     */
    @Query(
        """
        SELECT * FROM messages
        WHERE (:channelId = '' OR channelId = :channelId) AND isDeleted = 0
        ORDER BY createdAt DESC, id DESC
    """
    )
    fun pagingSource(channelId: String): PagingSource<Int, MessageEntity>

    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId AND createdAt < :beforeTimestamp AND isDeleted = 0
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
        WHERE channelId = :channelId AND createdAt > :afterTimestamp AND isDeleted = 0
        ORDER BY createdAt ASC 
        LIMIT :limit
    """
    )
    suspend fun getMessagesAfter(
        channelId: String,
        afterTimestamp: Long,
        limit: Int
    ): List<MessageEntity>

    // 사용하지 않는 메서드 제거됨: getMessagesBetween

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

    /**
     * 동일 채널/발신자/페이로드(내용)가 이미 존재하는지 확인하여 중복 업서트를 방지하기 위한 헬퍼.
     * 서버 반영본이 도착했을 때 낙관적 로컬본과 페이로드가 동일하면 기존 ID를 반환한다.
     */
    @Query(
        """
        SELECT id FROM messages
        WHERE channelId = :channelId
          AND senderId = :senderId
          AND payload = :payload
          AND isDeleted = 0
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    suspend fun findExistingIdBySenderAndPayload(
        channelId: String,
        senderId: String,
        payload: String
    ): String?

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
     * 특정 채널의 메시지 개수 조회 (삭제되지 않은 메시지만)
     */
    @Query("SELECT COUNT(*) FROM messages WHERE channelId = :channelId AND isDeleted = 0")
    suspend fun getMessageCountByChannel(channelId: String): Int

    /**
     * 특정 채널의 최신 메시지들 조회 (디버그용)
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId AND isDeleted = 0
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
