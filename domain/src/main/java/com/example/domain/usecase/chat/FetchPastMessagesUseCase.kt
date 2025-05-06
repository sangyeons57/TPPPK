package com.example.domain.usecase.chat

import com.example.domain.model.ChatMessage
import com.example.domain.repository.ChatRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 과거 채팅 메시지를 가져오는 UseCase
 * 
 * @property chatRepository 채팅 관련 기능을 제공하는 Repository
 */
class FetchPastMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * 특정 메시지 ID 이전의 채팅 메시지를 가져옵니다.
     *
     * @param channelId 채팅 채널 ID
     * @param beforeMessageId 이 메시지 ID 이전의 메시지를 가져옴
     * @param limit 가져올 메시지 수 제한
     * @return 성공 시 메시지 목록이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(channelId: String, beforeMessageId: Int, limit: Int): Result<List<ChatMessage>> {
        return chatRepository.fetchPastMessages(channelId, beforeMessageId, limit)
    }
} 