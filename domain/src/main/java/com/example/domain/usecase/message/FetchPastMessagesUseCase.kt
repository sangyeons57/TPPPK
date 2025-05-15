package com.example.domain.usecase.message

import com.example.domain.model.ChatMessage
import com.example.domain.repository.MessageRepository
import java.time.Instant
import javax.inject.Inject

/**
 * 이전 메시지 목록을 가져오는 유즈케이스입니다.
 * 페이지네이션을 지원하여 특정 시점 이전의 메시지를 로드합니다.
 */
class FetchPastMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * 지정된 채널에서 특정 시간 이전의 메시지를 가져옵니다.
     *
     * @param channelId 메시지를 가져올 채널의 ID.
     * @param before 이 시간점 이전의 메시지를 가져옵니다. null이면 가장 최근 메시지부터 가져오지만,
     *               이 유즈케이스는 '이전' 메시지를 가져오는 것이므로 일반적으로 null이 아닌 값이 기대됩니다.
     *               첫 로드 시에는 GetMessagesStreamUseCase를 사용하는 것을 고려하십시오.
     * @param limit 가져올 메시지의 최대 개수.
     * @return 메시지 목록을 담은 [Result].
     */
    suspend operator fun invoke(
        channelId: String,
        before: Instant?,
        limit: Int = 20 // Default limit for fetching past messages
    ): Result<List<ChatMessage>> {
        // 'before'가 null일 경우의 동작은 MessageRepository의 getMessages 스펙에 따름
        // (일반적으로 최신 메시지부터 가져옴, 이는 'past' 메시지 로드와는 다소 상이할 수 있음)
        // ChatViewModel 등 호출부에서 'before' 값을 명확히 관리해야 함.
        return messageRepository.getMessages(channelId = channelId, limit = limit, before = before)
    }
} 