package com.example.domain.usecase.message

import com.example.domain.repository.MessageRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.Result

/**
 * 메시지를 삭제하는 유즈케이스입니다. (논리적 삭제)
 * 메시지 작성자 또는 특정 권한을 가진 사용자만 메시지를 삭제할 수 있도록 권한 검사를 포함할 수 있습니다.
 */
class DeleteMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository // 작성자 또는 관리자 권한 확인용
) {
    /**
     * 지정된 메시지를 논리적으로 삭제합니다.
     *
     * @param channelId 메시지가 속한 채널의 ID.
     * @param messageId 삭제할 메시지의 ID.
     * @return 작업 성공 여부를 담은 [Result].
     */
    suspend operator fun invoke(channelId: String, messageId: String): Result<Unit> {
        val currentUserResult = userRepository.getCurrentUserStream().first()
        
        if (currentUserResult.isFailure) {
            return Result.failure(currentUserResult.exceptionOrNull() ?: IllegalStateException("Failed to get current user"))
        }
        val currentUser = currentUserResult.getOrNull()
            ?: return Result.failure(IllegalStateException("Current user not found or not available in Result"))
        
        val currentUserId = currentUser.id

        // 1. 원본 메시지 정보 가져오기 (작성자 확인용)
        val originalMessageResult = messageRepository.getMessage(channelId, messageId)
        if (originalMessageResult.isFailure) {
            return Result.failure(originalMessageResult.exceptionOrNull() ?: IllegalStateException("Original message not found, cannot verify sender for deletion."))
        }
        val originalMessage = originalMessageResult.getOrThrow()

        // 2. 권한 검사: 메시지 작성자 또는 채널 관리자/소유자만 삭제 가능 (프로젝트 정책에 따라 확장)
        if (originalMessage.senderId != currentUserId) {
            return Result.failure(SecurityException("User not authorized to delete this message."))
        }
        
        // 3. 이미 삭제된 메시지인지 확인 (선택적, 멱등성)
        if (originalMessage.isDeleted == true) {
            return Result.success(Unit) // 이미 삭제된 경우 성공으로 처리
        }

        // 4. 메시지 삭제 (논리적)
        return messageRepository.deleteMessage(channelId, messageId)
    }
} 