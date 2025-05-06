package com.example.domain.usecase.chat

import com.example.domain.model.ChatMessage
import com.example.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 채팅 메시지 스트림을 가져오는 UseCase
 * 
 * @property chatRepository 채팅 관련 기능을 제공하는 Repository
 */
class GetMessagesStreamUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * 특정 채널의 채팅 메시지 스트림을 가져옵니다.
     *
     * @param channelId 채팅 채널 ID
     * @return 채팅 메시지 Flow
     */
    operator fun invoke(channelId: String): Flow<List<ChatMessage>> {
        return chatRepository.getMessagesStream(channelId)
    }
} 