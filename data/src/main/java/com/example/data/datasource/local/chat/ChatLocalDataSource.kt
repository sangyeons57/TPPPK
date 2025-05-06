package com.example.data.datasource.local.chat

import com.example.data.model.local.ChatEntity
import com.example.data.model.local.chat.ChatMessageEntity
import com.example.data.model.local.MediaImageEntity
import com.example.data.model.remote.chat.ChatMessageDto
import kotlinx.coroutines.flow.Flow

/**
 * 채팅 메시지 데이터의 로컬 데이터 소스 인터페이스입니다.
 * 주로 Room 데이터베이스와 상호작용합니다.
 */
interface ChatLocalDataSource {
    // 특정 채널의 메시지 스트림 가져오기
    fun getMessagesStream(channelId: String): Flow<List<ChatMessageEntity>>

    // 특정 채널의 메시지 목록 저장 (페이징 고려)
    suspend fun saveMessages(channelId: String, messages: List<ChatMessageEntity>)

    // 단일 메시지 추가 또는 업데이트 (Upsert)
    suspend fun upsertMessage(message: ChatMessageEntity)

    // 메시지 삭제
    suspend fun deleteMessage(messageId: String)

    // 특정 채널의 모든 메시지 삭제
    suspend fun clearMessagesForChannel(channelId: String)

    /**
     * 채팅 메시지를 로컬 데이터베이스에 저장합니다.
     * 
     * @param message 저장할 채팅 메시지 DTO
     */
    suspend fun insertMessage(message: ChatMessageDto)
    
    /**
     * 여러 채팅 메시지를 로컬 데이터베이스에 저장합니다.
     * 
     * @param messages 저장할 채팅 메시지 DTO 목록
     */
    suspend fun insertMessages(messages: List<ChatMessageDto>)
    
    /**
     * 특정 채널의 모든 메시지를 가져옵니다.
     * 
     * @param channelId 채팅 채널 ID
     * @return 해당 채널의 모든 메시지 DTO 목록
     */
    suspend fun getAllMessages(channelId: String): List<ChatMessageDto>
    
    /**
     * 특정 메시지 ID 이전의 메시지를 가져옵니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param beforeMessageId 이 메시지 ID 이전의 메시지를 가져옴
     * @param limit 가져올 메시지 최대 개수
     * @return 메시지 DTO 목록
     */
    suspend fun getMessagesBefore(channelId: String, beforeMessageId: Int, limit: Int): List<ChatMessageDto>
    
    /**
     * 메시지 내용을 업데이트합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param chatId 수정할 메시지 ID
     * @param newMessage 새 메시지 내용
     */
    suspend fun updateMessage(channelId: String, chatId: Int, newMessage: String)
    
    /**
     * 메시지를 삭제합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param chatId 삭제할 메시지 ID
     */
    suspend fun deleteMessage(channelId: String, chatId: Int)
    
    /**
     * 로컬 갤러리 이미지를 페이징 방식으로 가져옵니다.
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param pageSize 페이지당 이미지 수
     * @return 미디어 이미지 엔티티 목록
     */
    suspend fun getLocalGalleryImages(page: Int, pageSize: Int): List<MediaImageEntity>

    // ... 향후 필요한 채팅 관련 로컬 데이터 처리 함수 추가 ...
} 