package com.example.data.datasource.local.chat

import com.example.data.db.dao.ChatDao // Room DAO 위치 가정
import com.example.data.model.local.ChatEntity
import com.example.data.model.local.chat.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatLocalDataSource 인터페이스의 Room 데이터베이스 구현체입니다.
 */
@Singleton
class ChatLocalDataSourceImpl @Inject constructor(
    private val chatDao: ChatDao // Room DAO 주입
) : ChatLocalDataSource {

    // --- ChatLocalDataSource 인터페이스 함수 구현 --- 

    /**
     * 특정 채널의 메시지 스트림을 가져옵니다.
     * 
     * @param channelId 메시지를 가져올 채널 ID
     * @return 시간순으로 정렬된 메시지 엔티티 목록의 Flow
     */
    override fun getMessagesStream(channelId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesStream(channelId)
    }

    /**
     * 특정 채널의 메시지 목록을 저장합니다.
     * 
     * @param channelId 메시지가 속한 채널 ID
     * @param messages 저장할 메시지 엔티티 목록
     */
    override suspend fun saveMessages(channelId: String, messages: List<ChatMessageEntity>) {
        // 채널 ID 확인
        val messagesForChannel = messages.map { 
            if (it.chatId != channelId) it.copy(chatId = channelId) else it 
        }
        
        chatDao.insertMessages(messagesForChannel)
    }

    /**
     * 단일 메시지를 추가하거나 업데이트합니다 (Upsert).
     * 
     * @param message 추가 또는 업데이트할 메시지 엔티티
     */
    override suspend fun upsertMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
    }

    /**
     * 메시지를 삭제합니다.
     * 
     * @param messageId 삭제할 메시지 ID
     */
    override suspend fun deleteMessage(messageId: String) {
        chatDao.deleteMessageById(messageId)
    }

    /**
     * 특정 채널의 모든 메시지를 삭제합니다.
     * 
     * @param channelId 메시지를 삭제할 채널 ID
     */
    override suspend fun clearMessagesForChannel(channelId: String) {
        chatDao.clearMessagesForChat(channelId)
    }

    // ... 다른 함수들의 실제 구현 추가 ...
} 