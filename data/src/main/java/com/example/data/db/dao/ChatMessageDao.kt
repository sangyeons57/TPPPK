package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.local.chat.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * 채팅 메시지 테이블에 대한 데이터 액세스 객체 인터페이스
 * Room 데이터베이스의 chat_messages 테이블과 상호작용합니다.
 */
@Dao
interface ChatMessageDao {
    
    /**
     * 특정 채널의 메시지 스트림을 가져옵니다.
     * 
     * @param channelId 채널 ID
     * @param channelType 채널 타입 ("DM", "PROJECT_CATEGORY", "PROJECT_DIRECT")
     * @return 해당 채널의 메시지 엔티티 리스트를 포함하는 Flow
     */
    @Query("SELECT * FROM chat_messages WHERE channelId = :channelId AND channelType = :channelType ORDER BY sentAt ASC")
    fun getMessagesStream(channelId: String, channelType: String): Flow<List<ChatMessageEntity>>
    
    /**
     * 특정 채널의 모든 메시지를 가져옵니다.
     * 
     * @param channelId 채널 ID
     * @param channelType 채널 타입
     * @return 해당 채널의 모든 메시지 엔티티 목록
     */
    @Query("SELECT * FROM chat_messages WHERE channelId = :channelId AND channelType = :channelType ORDER BY sentAt ASC")
    suspend fun getAllMessages(channelId: String, channelType: String): List<ChatMessageEntity>
    
    /**
     * 특정 메시지 ID 이전의 메시지를 가져옵니다.
     * 
     * @param channelId 채널 ID
     * @param channelType 채널 타입
     * @param beforeSentAt 이 타임스탬프 이전의 메시지를 가져옴 (milliseconds)
     * @param limit 가져올 메시지 최대 개수
     * @return 메시지 엔티티 목록
     */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE channelId = :channelId AND channelType = :channelType AND sentAt < :beforeSentAt 
        ORDER BY sentAt DESC 
        LIMIT :limit
    """)
    suspend fun getMessagesBefore(channelId: String, channelType: String, beforeSentAt: Long, limit: Int): List<ChatMessageEntity>
    
    /**
     * 메시지를 삽입하거나 교체합니다. chatId (Firestore ID) 기준이 아닌 PK(id) 기준.
     * 
     * @param message 삽입 또는 교체할 메시지 엔티티
     * @return 삽입된 행의 ID (교체 시에는 보통 0 또는 이전 rowid 반환, DB마다 다름)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceMessage(message: ChatMessageEntity): Long
    
    /**
     * 여러 메시지를 삽입하거나 교체합니다.
     * 
     * @param messages 삽입 또는 교체할 메시지 엔티티 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceMessages(messages: List<ChatMessageEntity>)
    
    /**
     * Firestore ID(chatId)로 메시지를 조회합니다.
     * @param chatId Firestore Document ID
     * @return ChatMessageEntity 또는 null
     */
    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId LIMIT 1")
    suspend fun getMessageByChatId(chatId: String): ChatMessageEntity?

    /**
     * 메시지를 추가하거나 업데이트합니다. Firestore ID(chatId) 기준.
     * 
     * @param message 추가 또는 업데이트할 메시지 엔티티
     */
    @Transaction
    suspend fun upsertMessageByChatId(message: ChatMessageEntity) {
        val existingMessage = getMessageByChatId(message.chatId)
        if (existingMessage != null) {
            // Update existing message, preserving the auto-generated primary key
            updateMessageEntity(message.copy(id = existingMessage.id))
        } else {
            // Insert new message (PK will be auto-generated)
            insertOrReplaceMessage(message)
        }
    }
    
    /**
     * 메시지 엔티티를 업데이트합니다. (Primary Key id 기준)
     * 
     * @param message 업데이트할 메시지 엔티티 (id 필드가 존재해야 함)
     */
    @Update
    suspend fun updateMessageEntity(message: ChatMessageEntity)
    
    /**
     * 메시지 내용을 업데이트합니다. (Firestore ID chatId 기준)
     * 
     * @param chatId 수정할 메시지의 Firestore ID
     * @param newMessage 새 메시지 내용
     * @param isModified 수정 여부 표시
     */
    @Query("""
        UPDATE chat_messages 
        SET message = :newMessage, isModified = :isModified 
        WHERE chatId = :chatId
    """)
    suspend fun updateMessageContentByChatId(chatId: String, newMessage: String, isModified: Boolean = true)
    
    /**
     * 로컬 ID(PK)로 메시지를 삭제합니다.
     * 
     * @param id 삭제할 메시지의 로컬 기본 키 ID
     */
    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageByLocalId(id: Long)
    
    /**
     * Firestore ID(chatId)로 메시지를 삭제합니다.
     * 
     * @param chatId 삭제할 메시지의 Firestore ID
     */
    @Query("DELETE FROM chat_messages WHERE chatId = :chatId")
    suspend fun deleteMessageByChatId(chatId: String)
    
    /**
     * 특정 채널의 모든 메시지를 삭제합니다.
     * 
     * @param channelId 메시지를 삭제할 채널 ID
     * @param channelType 메시지를 삭제할 채널 타입
     */
    @Query("DELETE FROM chat_messages WHERE channelId = :channelId AND channelType = :channelType")
    suspend fun clearMessagesForChannel(channelId: String, channelType: String)
} 