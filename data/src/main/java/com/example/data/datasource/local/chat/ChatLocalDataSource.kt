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
    /**
     * 특정 채널의 메시지 스트림을 가져옵니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param channelType 채널 유형 (예: "DM", "PROJECT_CATEGORY")
     * @return 채팅 메시지 엔티티 리스트를 포함하는 Flow
     */
    fun getMessagesStream(channelId: String, channelType: String): Flow<List<ChatMessageEntity>>

    /**
     * 특정 채널의 메시지 목록을 저장합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param channelType 채널 유형
     * @param messages 저장할 채팅 메시지 엔티티 목록
     */
    suspend fun saveMessages(channelId: String, channelType: String, messages: List<ChatMessageEntity>)

    /**
     * 단일 메시지를 추가하거나 업데이트합니다. (chatId 기준)
     * ChatMessageEntity는 channelId와 channelType을 이미 가지고 있어야 합니다.
     *
     * @param message 추가 또는 업데이트할 채팅 메시지 엔티티
     */
    suspend fun upsertMessage(message: ChatMessageEntity)

    /**
     * 메시지를 삭제합니다. (chatId 기준)
     *
     * @param chatId 삭제할 메시지의 Firestore ID
     */
    suspend fun deleteMessage(chatId: String)

    /**
     * 특정 채널의 모든 메시지를 삭제합니다.
     *
     * @param channelId 채팅 채널 ID
     * @param channelType 채널 유형
     */
    suspend fun clearMessagesForChannel(channelId: String, channelType: String)

    /**
     * 채팅 메시지를 로컬 데이터베이스에 저장합니다.
     *
     * @param message 저장할 채팅 메시지 DTO
     * @param channelType 이 메시지가 속한 채널의 유형
     */
    suspend fun insertMessage(message: ChatMessageDto, channelType: String)

    /**
     * 여러 채팅 메시지를 로컬 데이터베이스에 저장합니다.
     *
     * @param messages 저장할 채팅 메시지 DTO 목록
     * @param channelType 이 메시지들이 속한 채널의 유형
     */
    suspend fun insertMessages(messages: List<ChatMessageDto>, channelType: String)

    /**
     * 특정 채널의 모든 메시지를 가져옵니다.
     *
     * @param channelId 채팅 채널 ID
     * @param channelType 채널 유형
     * @return 해당 채널의 모든 메시지 DTO 목록
     */
    suspend fun getAllMessages(channelId: String, channelType: String): List<ChatMessageDto>

    /**
     * 특정 시간 이전의 메시지를 가져옵니다.
     *
     * @param channelId 채팅 채널 ID
     * @param channelType 채널 유형
     * @param beforeSentAt 이 타임스탬프 이전의 메시지를 가져옴 (milliseconds)
     * @param limit 가져올 메시지 최대 개수
     * @return 메시지 DTO 목록
     */
    suspend fun getMessagesBefore(channelId: String, channelType: String, beforeSentAt: Long, limit: Int): List<ChatMessageDto>

    /**
     * 메시지 내용을 업데이트합니다.
     *
     * @param chatId 수정할 메시지의 Firestore ID
     * @param newMessage 새 메시지 내용
     */
    suspend fun updateMessageContent(chatId: String, newMessage: String)

    /**
     * 메시지를 삭제합니다. (Firestore ID chatId 기준)
     *
     * @param chatId 삭제할 메시지의 Firestore ID
     */
    suspend fun deleteMessageByChatId(chatId: String)
    
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