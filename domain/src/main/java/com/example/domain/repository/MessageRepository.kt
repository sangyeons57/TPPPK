package com.example.domain._repository

import com.example.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlin.Result

// 메시지 전송 시 사용할 첨부파일 모델 (도메인 모델 MessageAttachment와 구분)
data class MessageAttachmentToSend(
    val fileName: String,
    val mimeType: String,
    val sourceUri: String // 예시: content URI 또는 file URI
    // val bytes: ByteArray? // 또는 직접 바이트를 전달할 경우
)

/**
 * 채널 내 메시지 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface MessageRepository {
    /**
     * 특정 채널의 메시지 목록을 실시간 스트림으로 가져옵니다.
     * @param channelId 메시지를 가져올 채널 ID
     * @param limit 한 번에 가져올 메시지 개수 (최신 메시지부터)
     * @return 메시지 목록을 담은 Result Flow.
     */
    fun getMessagesStream(channelId: String, limit: Int): Flow<Result<List<ChatMessage>>>

    /**
     * 특정 채널의 과거 메시지를 가져옵니다 (페이지네이션).
     * @param channelId 메시지를 가져올 채널 ID
     * @param startAfterMessageId 이 메시지 ID 이후(과거 방향)의 메시지를 가져옴 (null이면 가장 최신부터)
     * @param limit 한 번에 가져올 메시지 개수
     * @return 과거 메시지 목록을 담은 Result.
     */
    suspend fun getPastMessages(channelId: String, startAfterMessageId: String?, limit: Int): Result<List<ChatMessage>>

    /**
     * 새로운 메시지를 전송합니다.
     * @param channelId 메시지를 전송할 채널 ID
     * @param content 메시지 내용 (텍스트)
     * @param attachments 첨부 파일 목록 (선택적)
     * @param senderId 발신자 ID
     * @return 생성된 메시지의 ID 또는 성공 여부를 담은 Result.
     */
    suspend fun sendMessage(
        channelId: String,
        content: String?,
        attachments: List<MessageAttachmentToSend>,
        senderId: String
    ): Result<String>

    /**
     * 기존 메시지를 수정합니다.
     * @param channelId 메시지가 있는 채널 ID
     * @param messageId 수정할 메시지 ID
     * @param newContent 새로운 메시지 내용
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun editMessage(
        channelId: String,
        messageId: String,
        newContent: String
    ): Result<Unit>

    /**
     * 메시지를 삭제합니다.
     * @param channelId 메시지가 있는 채널 ID
     * @param messageId 삭제할 메시지 ID
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun deleteMessage(channelId: String, messageId: String): Result<Unit>

    /**
     * 메시지에 리액션을 추가합니다.
     * @param channelId 메시지가 있는 채널 ID
     * @param messageId 리액션을 추가할 메시지 ID
     * @param reactionEmoji 리액션 이모지 (String)
     * @param userId 리액션을 추가하는 사용자 ID
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun addReaction(channelId: String, messageId: String, reactionEmoji: String, userId: String): Result<Unit>

    /**
     * 메시지에서 리액션을 제거합니다.
     * @param channelId 메시지가 있는 채널 ID
     * @param messageId 리액션을 제거할 메시지 ID
     * @param reactionEmoji 리액션 이모지 (String)
     * @param userId 리액션을 제거하는 사용자 ID
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun removeReaction(channelId: String, messageId: String, reactionEmoji: String, userId: String): Result<Unit>

    /**
     * 특정 메시지 정보를 가져옵니다.
     * @param channelId 메시지가 있는 채널 ID
     * @param messageId 가져올 메시지 ID
     * @return 해당 메시지 정보를 담은 Result
     */
    suspend fun getMessage(channelId: String, messageId: String): Result<ChatMessage>
}
