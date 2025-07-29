package com.example.domain.usecase.local.messages

import com.example.domain.model.Message
import com.example.domain.repository.local.MessageLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetMessagesUseCase {
    operator fun invoke(channelId: String): Flow<List<Message>>
}

class GetMessagesUseCaseImpl @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository
) : GetMessagesUseCase {

    override operator fun invoke(channelId: String): Flow<List<Message>> {
        return TODO("특정 채널의 메시지 목록을 실시간으로 관찰")
    }
} 