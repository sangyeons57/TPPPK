package com.example.domain.usecase.message

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.ChatMessage
import com.example.domain.repository.MessageRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import kotlin.Result

/**
 * 메시지 내용을 수정하는 유즈케이스입니다.
 * 메시지 작성자만 메시지를 수정할 수 있도록 권한 검사를 포함할 수 있습니다.
 */
class EditMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository // 작성자 확인을 위해 UserRepository 주입
) {
    /**
     * 지정된 메시지의 내용을 수정합니다.
     *
     * @param channelId 메시지가 속한 채널의 ID.
     * @param messageId 수정할 메시지의 ID.
     * @param newText 새로운 메시지 내용.
     * @return 작업 성공 여부를 담은 [Result].
     */
    suspend operator fun invoke(
        channelId: String, // channelId는 getMessage에 필요
        messageId: String,
        newText: String
    ): Result<Unit> {
        val currentUserResult = userRepository.getCurrentUser().first()

        if (currentUserResult.isFailure) {
            return Result.failure(currentUserResult.exceptionOrNull() ?: IllegalStateException("Failed to get current user"))
        }
        val currentUser = currentUserResult.getOrNull()
            ?: return Result.failure(IllegalStateException("Current user not found or not available in Result"))
        
        val currentUserId = currentUser.id

        // 1. 원본 메시지 가져오기
        val originalMessageResult = messageRepository.getMessage(channelId, messageId)
        if (originalMessageResult.isFailure) {
            return Result.failure(originalMessageResult.exceptionOrNull() ?: IllegalStateException("Original message not found"))
        }
        val originalMessage = originalMessageResult.getOrThrow()

        // 2. 권한 검사: 메시지 작성자만 수정 가능
        if (originalMessage.senderId != currentUserId) {
            return Result.failure(SecurityException("User not authorized to edit this message."))
        }

        // 3. 이미 삭제된 메시지인지 확인 (선택적)
        if (originalMessage.isDeleted == true) {
            return Result.failure(IllegalStateException("Cannot edit a deleted message."))
        }

        // 4. 메시지 업데이트
        val updatedMessage = originalMessage.copy(
            text = newText,
            isEdited = true,
            updatedAt = DateTimeUtil.nowInstant()
        )
        return messageRepository.updateMessage(updatedMessage)
    }
} 