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
     * @param channelId 채팅 채널 ID
     * @return 해당 채널의 메시지 엔티티 리스트를 포함하는 Flow
     */
    @Query("SELECT * FROM chat_messages WHERE channelId = :channelId ORDER BY sentAt ASC")
    fun getMessagesStream(channelId: String): Flow<List<ChatMessageEntity>>
    
    /**
     * 특정 채널의 모든 메시지를 가져옵니다.
     * 
     * @param channelId 채팅 채널 ID
     * @return 해당 채널의 모든 메시지 엔티티 목록
     */
    @Query("SELECT * FROM chat_messages WHERE channelId = :channelId ORDER BY sentAt ASC")
    suspend fun getAllMessages(channelId: String): List<ChatMessageEntity>
    
    /**
     * 특정 메시지 ID 이전의 메시지를 가져옵니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param chatId 이 메시지 ID 이전의 메시지를 가져옴
     * @param limit 가져올 메시지 최대 개수
     * @return 메시지 엔티티 목록
     */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE channelId = :channelId AND chatId < :chatId 
        ORDER BY sentAt DESC 
        LIMIT :limit
    """)
    suspend fun getMessagesBefore(channelId: String, chatId: Int, limit: Int): List<ChatMessageEntity>
    
    /**
     * 메시지를 삽입합니다.
     * 
     * @param message 삽입할 메시지 엔티티
     * @return 삽입된 행의 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long
    
    /**
     * 여러 메시지를 삽입합니다.
     * 
     * @param messages 삽입할 메시지 엔티티 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)
    
    /**
     * 메시지를 추가하거나 업데이트합니다.
     * 
     * @param message 추가 또는 업데이트할 메시지 엔티티
     */
    @Transaction
    suspend fun upsertMessage(message: ChatMessageEntity) {
        val id = insertMessage(message)
        if (id == -1L) {
            updateMessageEntity(message)
        }
    }
    
    /**
     * 메시지 엔티티를 업데이트합니다.
     * 
     * @param message 업데이트할 메시지 엔티티
     */
    @Update
    suspend fun updateMessageEntity(message: ChatMessageEntity)
    
    /**
     * 메시지 내용을 업데이트합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param chatId 수정할 메시지 ID
     * @param newMessage 새 메시지 내용
     * @param isModified 수정 여부 표시
     */
    @Query("""
        UPDATE chat_messages 
        SET message = :newMessage, isModified = :isModified 
        WHERE channelId = :channelId AND chatId = :chatId
    """)
    suspend fun updateMessage(channelId: String, chatId: Int, newMessage: String, isModified: Boolean = true)
    
    /**
     * ID로 메시지를 삭제합니다.
     * 
     * @param messageId 삭제할 메시지의 로컬 ID
     */
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
    
    /**
     * chatId와 channelId로 메시지를 삭제합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param chatId 삭제할 메시지의 채팅 ID
     */
    @Query("DELETE FROM chat_messages WHERE channelId = :channelId AND chatId = :chatId")
    suspend fun deleteMessageByChatId(channelId: String, chatId: Int)
    
    /**
     * 특정 채널의 모든 메시지를 삭제합니다.
     * 
     * @param channelId 메시지를 삭제할 채널 ID
     */
    @Query("DELETE FROM chat_messages WHERE channelId = :channelId")
    suspend fun clearMessagesForChannel(channelId: String)
} 