package com.example.data.datasource.remote.chat

import com.example.data.model.remote.chat.ChatMessageDto
import com.example.data.model.remote.media.MediaImageDto
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 원격 채팅 데이터 소스 인터페이스
 * Firebase Firestore를 사용한 채팅 관련 원격 데이터 액세스를 정의합니다.
 */
interface ChatRemoteDataSource {
    /**
     * 특정 채널의 메시지 스트림을 실시간으로 가져옵니다.
     * 
     * @param channelId 채팅 채널 ID
     * @return 채팅 메시지 DTO 리스트를 포함하는 Flow
     */
    fun getMessagesStream(channelId: String): Flow<List<ChatMessageDto>>
    
    /**
     * 특정 메시지 ID 이전의 과거 메시지를 페이징 방식으로 가져옵니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param beforeMessageId 이 메시지 ID 이전의 메시지를 가져옴
     * @param limit 가져올 메시지 최대 개수
     * @return 과거 메시지 DTO 리스트를 포함한 Result
     */
    suspend fun fetchPastMessages(channelId: String, beforeMessageId: Int, limit: Int): Result<List<ChatMessageDto>>
    
    /**
     * 새 메시지를 전송합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param message 메시지 내용
     * @param attachmentPaths 첨부 이미지 경로 목록
     * @return 전송된 메시지 DTO를 포함한 Result
     */
    suspend fun sendMessage(channelId: String, message: String, attachmentPaths: List<String>): Result<ChatMessageDto>
    
    /**
     * 기존 메시지를 수정합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param chatId 수정할 메시지 ID
     * @param newMessage 새 메시지 내용
     * @return 작업 결과
     */
    suspend fun editMessage(channelId: String, chatId: Int, newMessage: String): Result<Unit>
    
    /**
     * 메시지를 삭제합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param chatId 삭제할 메시지 ID
     * @return 작업 결과
     */
    suspend fun deleteMessage(channelId: String, chatId: Int): Result<Unit>
    
    /**
     * 로컬 갤러리 이미지를 페이징 방식으로 가져옵니다.
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param pageSize 페이지당 이미지 수
     * @return 미디어 이미지 DTO 목록
     */
    suspend fun getLocalGalleryImages(page: Int, pageSize: Int): List<MediaImageDto>
} 