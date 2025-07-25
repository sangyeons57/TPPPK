package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 채팅 메시지 로컬 저장을 위한 DAO
 * updateAt 기반 증분 동기화와 효율적인 페이지네이션을 지원
 */
@Dao
interface ChatMessageDao {

    /**
     * 채널의 최신 메시지들을 가져옴 (초기 로딩용)
     * @param channelId 채널 ID
     * @param limit 가져올 메시지 개수
     * @return 생성시간 내림차순으로 정렬된 메시지 목록
     */
    @Query(
        """
        SELECT * FROM chat_messages 
        WHERE channelId = :channelId 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """
    )
    suspend fun getLatestMessages(channelId: String, limit: Int): List<ChatMessageEntity>

    /**
     * 특정 시점 이후 업데이트된 메시지들을 가져옴 (증분 동기화용)
     * 🔑 핵심: updateAt > timestamp 조건으로 변경된 메시지만 효율적으로 가져옴
     * @param channelId 채널 ID
     * @param timestamp 마지막 동기화 시간
     * @return 업데이트 시간 오름차순으로 정렬된 메시지 목록
     */
    @Query(
        """
        SELECT * FROM chat_messages 
        WHERE channelId = :channelId 
        AND updatedAt > :timestamp 
        ORDER BY updatedAt ASC
    """
    )
    suspend fun getUpdatedMessages(channelId: String, timestamp: Instant): List<ChatMessageEntity>

    /**
     * 특정 시점 이전의 과거 메시지들을 가져옴 (페이지네이션용)
     * @param channelId 채널 ID
     * @param beforeTimestamp 기준 시간
     * @param limit 가져올 메시지 개수
     * @return 생성시간 내림차순으로 정렬된 과거 메시지 목록
     */
    @Query(
        """
        SELECT * FROM chat_messages 
        WHERE channelId = :channelId 
        AND createdAt < :beforeTimestamp 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """
    )
    suspend fun getMessagesBefore(
        channelId: String,
        beforeTimestamp: Instant,
        limit: Int
    ): List<ChatMessageEntity>

    /**
     * 특정 시점 이후의 최신 메시지들을 가져옴 (새 메시지 확인용)
     * @param channelId 채널 ID
     * @param afterTimestamp 기준 시간
     * @param limit 가져올 메시지 개수
     * @return 생성시간 오름차순으로 정렬된 최신 메시지 목록
     */
    @Query(
        """
        SELECT * FROM chat_messages 
        WHERE channelId = :channelId 
        AND createdAt > :afterTimestamp 
        ORDER BY createdAt ASC 
        LIMIT :limit
    """
    )
    suspend fun getMessagesAfter(
        channelId: String,
        afterTimestamp: Instant,
        limit: Int
    ): List<ChatMessageEntity>

    /**
     * 채널의 메시지들을 실시간으로 관찰 (Flow)
     * @param channelId 채널 ID
     * @param limit 최대 메시지 개수
     * @return 생성시간 내림차순으로 정렬된 메시지 Flow
     */
    @Query(
        """
        SELECT * FROM chat_messages 
        WHERE channelId = :channelId 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """
    )
    fun observeMessages(channelId: String, limit: Int): Flow<List<ChatMessageEntity>>

    /**
     * 메시지 배치 삽입/업데이트
     * @param messages 저장할 메시지들
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    /**
     * 단일 메시지 삽입/업데이트
     * @param message 저장할 메시지
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    /**
     * 메시지 업데이트 (편집, 삭제 처리용)
     * @param message 업데이트할 메시지
     */
    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    /**
     * 특정 메시지 ID로 메시지 조회
     * @param messageId 메시지 ID
     * @return 해당 메시지 (없으면 null)
     */
    @Query("SELECT * FROM chat_messages WHERE messageId = :messageId")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?

    /**
     * 채널의 메시지 개수 조회
     * @param channelId 채널 ID
     * @return 메시지 개수
     */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE channelId = :channelId")
    suspend fun getMessageCount(channelId: String): Int

    /**
     * 채널의 가장 오래된 메시지 시간 조회
     * @param channelId 채널 ID
     * @return 가장 오래된 메시지의 생성 시간
     */
    @Query("SELECT MIN(createdAt) FROM chat_messages WHERE channelId = :channelId")
    suspend fun getOldestMessageTimestamp(channelId: String): Instant?

    /**
     * 채널의 가장 최신 메시지 시간 조회
     * @param channelId 채널 ID
     * @return 가장 최신 메시지의 생성 시간
     */
    @Query("SELECT MAX(createdAt) FROM chat_messages WHERE channelId = :channelId")
    suspend fun getNewestMessageTimestamp(channelId: String): Instant?

    /**
     * 오래된 메시지 삭제 (메모리 관리용)
     * @param channelId 채널 ID
     * @param oldestTimestamp 삭제할 기준 시간
     * @return 삭제된 메시지 개수
     */
    @Query("DELETE FROM chat_messages WHERE channelId = :channelId AND createdAt <= :oldestTimestamp")
    suspend fun deleteOldMessages(channelId: String, oldestTimestamp: Instant): Int

    /**
     * 채널의 모든 메시지 삭제
     * @param channelId 채널 ID
     */
    @Query("DELETE FROM chat_messages WHERE channelId = :channelId")
    suspend fun deleteAllMessages(channelId: String)

    /**
     * 모든 채널의 모든 메시지 삭제 (앱 데이터 초기화용)
     */
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessagesFromAllChannels()

    /**
     * 특정 메시지 존재 여부 확인
     * @param messageId 메시지 ID
     * @return 존재 여부
     */
    @Query("SELECT EXISTS(SELECT 1 FROM chat_messages WHERE messageId = :messageId)")
    suspend fun messageExists(messageId: String): Boolean

    /**
     * [DEBUG] 모든 채널의 모든 메시지를 가져옴
     * @return 모든 메시지 목록 (채널별, 시간순 정렬)
     */
    @Query("SELECT * FROM chat_messages ORDER BY channelId, createdAt DESC")
    suspend fun getAllMessagesForDebug(): List<ChatMessageEntity>

    /**
     * [DEBUG] 특정 채널의 모든 메시지를 가져옴
     * @param channelId 채널 ID
     * @return 해당 채널의 모든 메시지 목록 (시간순 정렬)
     */
    @Query("SELECT * FROM chat_messages WHERE channelId = :channelId ORDER BY createdAt DESC")
    suspend fun getAllMessagesForChannelForDebug(channelId: String): List<ChatMessageEntity>
}