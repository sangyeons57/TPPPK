package com.example.domain.usecase.message

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.ChatMessage
import com.example.domain.model.MessageAttachment
import com.example.domain.model.User
import com.example.domain.repository.MessageRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.Result

/**
 * 현재 로그인된 사용자의 메시지를 전송하는 유즈케이스입니다.
 * 메시지 객체를 내부적으로 생성하고 발신자 정보를 자동으로 채웁니다.
 *
 * @property messageRepository 메시지 데이터 처리를 위한 리포지토리
 * @property userRepository 현재 사용자 정보 조회를 위한 리포지토리
 */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) {
    /**
     * 메시지를 전송합니다.
     *
     * @param channelId 메시지를 보낼 채널 ID.
     * @param text 메시지 텍스트 내용.
     * @param attachments 첨부 파일 목록 (옵션).
     * @param replyToMessageId 답장할 메시지의 ID (옵션).
     * @return 전송된 메시지 정보 (서버에서 ID, 타임스탬프 등이 채워짐) 또는 실패 Result.
     */
    suspend operator fun invoke(
        channelId: String,
        text: String,
        attachments: List<MessageAttachment> = emptyList(),
        replyToMessageId: String? = null
    ): Result<ChatMessage> {
        // 현재 로그인된 사용자 정보 가져오기
        val currentUserResult: Result<User?> = userRepository.getCurrentUserStream().first()
        
        val currentUser: User = currentUserResult.getOrNull() 
            ?: return Result.failure(
                currentUserResult.exceptionOrNull() ?: IllegalStateException("Current user not found or failed to load.")
            )

        // ChatMessage 객체 생성
        val message = ChatMessage(
            id = "", // Repository/DataSource에서 생성됨
            channelId = channelId,
            senderId = currentUser.id,
            senderName = currentUser.name.ifEmpty { currentUser.email.ifEmpty { "Unknown User" } },
            senderProfileUrl = currentUser.profileImageUrl,
            text = text,
            timestamp = DateTimeUtil.nowInstant(),
            attachments = attachments,
            replyToMessageId = replyToMessageId,
            isEdited = false,
            isDeleted = false,
            reactions = emptyMap(),
            metadata = null,
            updatedAt = null
        )
        return messageRepository.sendMessage(message)
    }
} 