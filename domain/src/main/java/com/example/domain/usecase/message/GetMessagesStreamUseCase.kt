package com.example.domain.usecase.message

import com.example.domain.model.ChatMessage
import com.example.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 채널의 메시지 스트림을 가져오는 유스케이스입니다.
 *
 * @property messageRepository 메시지 데이터 처리를 위한 리포지토리
 */
class GetMessagesStreamUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(channelId: String, limit: Int = 50): Flow<List<ChatMessage>> {
        return messageRepository.getMessagesStream(channelId, limit)
    }
} 