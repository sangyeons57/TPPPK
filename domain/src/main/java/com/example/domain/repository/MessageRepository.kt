package com.example.domain.repository

import com.example.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.time.Instant
import kotlin.Result

/**
 * 메시지 관련 데이터 작업을 위한 리포지토리 인터페이스입니다.
 * 이 인터페이스는 특정 채널의 메시지 스트림 구독, 과거 메시지 로드,
 * 메시지 전송, 수정, 삭제 및 파일 업로드 기능을 포함합니다.
 */
interface MessageRepository {

    /**
     * 특정 채널의 메시지 스트림을 구독합니다.
     * 최신 메시지부터 지정된 수만큼 가져오며, 이후 새 메시지가 추가되면 스트림을 통해 전달받습니다.
     *
     * @param channelId 메시지를 가져올 채널의 ID.
     * @param limit 초기에 가져올 메시지 수.
     * @return ChatMessage 객체의 Flow.
     */
    fun getMessagesStream(channelId: String, limit: Int): Flow<List<ChatMessage>>

    /**
     * 특정 채널의 과거 메시지를 가져옵니다.
     * 지정된 시간 이전의 메시지를 지정된 수만큼 가져옵니다.
     *
     * @param channelId 메시지를 가져올 채널의 ID.
     * @param before 이 시간 이전에 전송된 메시지를 가져옵니다 (페이징 기준점).
     * @param limit 가져올 메시지 수.
     * @return ChatMessage 객체의 리스트 또는 실패 Result.
     */
    suspend fun getPastMessages(
        channelId: String,
        before: Instant?,
        limit: Int
    ): Result<List<ChatMessage>>

    /**
     * 새 메시지를 전송합니다.
     *
     * @param message 전송할 메시지 정보. ID는 서버에서 생성될 수 있습니다.
     * @return 전송된 메시지 정보 (서버에서 ID, 타임스탬프 등이 채워짐) 또는 실패 Result.
     */
    suspend fun sendMessage(message: ChatMessage): Result<ChatMessage>

    /**
     * 기존 메시지를 수정합니다.
     *
     * @param message 수정할 메시지 정보. ID는 필수입니다.
     * @return 작업 성공 여부.
     */
    suspend fun updateMessage(message: ChatMessage): Result<Unit>

    /**
     * 메시지를 삭제합니다. (소프트 삭제 또는 하드 삭제는 구현에 따라 다름)
     *
     * @param channelId 메시지가 속한 채널 ID.
     * @param messageId 삭제할 메시지 ID.
     * @return 작업 성공 여부.
     */
    suspend fun deleteMessage(channelId: String, messageId: String): Result<Unit>
    
    /**
     * 특정 메시지 정보를 가져옵니다.
     * @param channelId 메시지가 속한 채널 ID
     * @param messageId 가져올 메시지 ID
     * @return 메시지 정보 또는 실패 Result
     */
    suspend fun getMessage(channelId: String, messageId: String): Result<ChatMessage>

    /**
     * 특정 채널의 메시지를 가져옵니다.
     * 
     * @param channelId 메시지를 가져올 채널 ID.
     * @param limit 가져올 메시지 수.
     * @param before 이 시간 이전에 전송된 메시지를 가져옵니다 (페이징 기준점).
     * @return ChatMessage 객체의 리스트 또는 실패 Result.
     */
    suspend fun getMessages(channelId: String, limit: Int, before: Instant?): Result<List<ChatMessage>>

    // --- 파일 업로드 관련 (선택적, 필요시 구현) ---
    /**
     * 채팅 메시지에 첨부할 파일을 업로드합니다.
     *
     * @param channelId 파일이 첨부될 채널 ID.
     * @param fileName 파일 이름.
     * @param inputStream 파일 데이터 스트림.
     * @param mimeType 파일의 MIME 타입.
     * @return 업로드된 파일의 URL 또는 실패 Result.
     */
    suspend fun uploadChatFile(
        channelId: String,
        fileName: String,
        inputStream: InputStream,
        mimeType: String
    ): Result<String> // Returns download URL
    
    // --- 로컬 데이터베이스 관련 (선택적, 필요시 구현) ---
    /**
     * 로컬에 캐시된 메시지를 가져옵니다. (오프라인 지원용)
     *
     * @param channelId 대상 채널 ID.
     * @param limit 가져올 메시지 수.
     * @return 캐시된 ChatMessage 목록.
     */
    suspend fun getCachedMessages(channelId: String, limit: Int): Flow<List<ChatMessage>>

    /**
     * 메시지를 로컬 캐시에 저장합니다.
     *
     * @param messages 저장할 ChatMessage 목록.
     */
    suspend fun saveMessagesToCache(messages: List<ChatMessage>): Result<Unit>

    /**
     * 특정 채널의 모든 캐시된 메시지를 삭제합니다.
     *
     * @param channelId 대상 채널 ID.
     */
    suspend fun clearCachedMessagesForChannel(channelId: String): Result<Unit>
}
