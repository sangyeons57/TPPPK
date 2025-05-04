package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.data.model.local.ChatEntity
import com.example.data.model.local.chat.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room 데이터베이스의 'chats' 및 'chat_messages' 테이블에 접근하기 위한 DAO 인터페이스입니다.
 */
@Dao
interface ChatDao {

    // --- Chat Room (ChatEntity) Methods ---

    /**
     * 특정 프로젝트에 속한 모든 채팅방 목록을 Flow 형태로 가져옵니다.
     * 마지막 메시지 시간 기준으로 내림차순 정렬합니다.
     * @param projectId 채팅방 목록을 가져올 프로젝트의 ID.
     * @return 채팅방 엔티티 리스트의 Flow.
     */
    @Query("SELECT * FROM chats WHERE projectId = :projectId ORDER BY lastMessageTimestamp DESC")
    fun getChatsStream(projectId: String): Flow<List<ChatEntity>>

    /**
     * 사용자가 참여하고 있는 모든 채팅방 목록을 Flow 형태로 가져옵니다.
     * 마지막 메시지 시간 기준으로 내림차순 정렬합니다.
     * @param userId 사용자 ID.
     * @return 채팅방 엔티티 리스트의 Flow.
     */
    @Query("SELECT * FROM chats WHERE participantIds LIKE '%' || :userId || '%' ORDER BY lastMessageTimestamp DESC")
    fun getUserChatsStream(userId: String): Flow<List<ChatEntity>>

    /**
     * 특정 ID를 가진 채팅방 정보를 가져옵니다.
     * @param chatId 가져올 채팅방의 ID.
     * @return 해당 ID의 채팅방 엔티티. 없으면 null.
     */
    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    /**
     * 단일 채팅방 정보를 삽입하거나 이미 존재하면 업데이트합니다 (Upsert).
     * @param chat 추가 또는 업데이트할 채팅방 엔티티.
     */
    @Upsert
    suspend fun upsertChat(chat: ChatEntity)

    /**
     * 여러 채팅방 정보를 한 번에 삽입합니다. 충돌 시 무시합니다.
     * @param chats 삽입할 채팅방 엔티티 리스트.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChats(chats: List<ChatEntity>)

    /**
     * 채팅방 정보를 업데이트합니다.
     * @param chat 업데이트할 채팅방 엔티티.
     * @return 업데이트된 행의 수.
     */
    @Update
    suspend fun updateChat(chat: ChatEntity): Int

    /**
     * 특정 ID의 채팅방을 삭제합니다.
     * @param chatId 삭제할 채팅방의 ID.
     * @return 삭제된 행의 수.
     */
    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String): Int

    /**
     * 채팅방을 삭제합니다.
     * @param chat 삭제할 채팅방 엔티티.
     * @return 삭제된 행의 수.
     */
    @Delete
    suspend fun deleteChat(chat: ChatEntity): Int

    /**
     * 특정 프로젝트의 모든 채팅방을 삭제합니다.
     * @param projectId 채팅방을 삭제할 프로젝트의 ID.
     */
    @Query("DELETE FROM chats WHERE projectId = :projectId")
    suspend fun clearProjectChats(projectId: String)

    /**
     * 모든 채팅방을 삭제합니다.
     */
    @Query("DELETE FROM chats")
    suspend fun clearAllChats()

    // --- Chat Message (ChatMessageEntity) Methods ---

    /**
     * 특정 채팅방의 모든 메시지 목록을 Flow 형태로 가져옵니다.
     * 메시지 발신 시간 기준으로 오름차순 정렬합니다.
     * @param chatId 메시지를 가져올 채팅방의 ID.
     * @return 메시지 엔티티 리스트의 Flow.
     */
    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY sentAt ASC")
    fun getMessagesStream(chatId: String): Flow<List<ChatMessageEntity>>

    /**
     * 특정 ID를 가진 메시지 정보를 가져옵니다.
     * @param messageId 가져올 메시지의 ID.
     * @return 해당 ID의 메시지 엔티티. 없으면 null.
     */
    @Query("SELECT * FROM chat_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?

    /**
     * 단일 메시지를 삽입합니다. 충돌 시 대체합니다.
     * @param message 삽입할 메시지 엔티티.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    /**
     * 여러 메시지를 한 번에 삽입합니다. 충돌 시 대체합니다.
     * @param messages 삽입할 메시지 엔티티 리스트.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    /**
     * 메시지를 삭제합니다.
     * @param message 삭제할 메시지 엔티티.
     * @return 삭제된 행의 수.
     */
    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity): Int

    /**
     * 특정 ID의 메시지를 삭제합니다.
     * @param messageId 삭제할 메시지의 ID.
     * @return 삭제된 행의 수.
     */
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String): Int

    /**
     * 특정 채팅방의 모든 메시지를 삭제합니다.
     * @param chatId 메시지를 삭제할 채팅방의 ID.
     */
    @Query("DELETE FROM chat_messages WHERE chatId = :chatId")
    suspend fun clearMessagesForChat(chatId: String)

    /**
     * 모든 메시지를 삭제합니다.
     */
    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
} 