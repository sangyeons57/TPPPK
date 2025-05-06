package com.example.domain.usecase.chat

import com.example.domain.repository.ChatRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 채팅 메시지를 수정하는 UseCase
 * 
 * @property chatRepository 채팅 관련 기능을 제공하는 Repository
 */
class EditMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * 기존 채팅 메시지를 수정합니다.
     *
     * @param channelId 채팅 채널 ID
     * @param messageId 수정할 메시지 ID
     * @param newMessage 새 메시지 내용
     * @return 성공 시 성공 결과가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(channelId: String, messageId: Int, newMessage: String): Result<Unit> {
        return chatRepository.editMessage(channelId, messageId, newMessage)
    }
} 