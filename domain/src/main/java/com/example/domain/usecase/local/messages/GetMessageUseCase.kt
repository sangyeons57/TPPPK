package com.example.domain.usecase.local.messages

import com.example.domain.model.Message
import com.example.domain.repository.local.MessageLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetMessageUseCase {
    operator fun invoke(messageId: String): Flow<Message?>
}

class GetMessageUseCaseImpl @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository
) : GetMessageUseCase {

    override operator fun invoke(messageId: String): Flow<Message?> {
        return TODO("특정 메시지 정보를 실시간으로 관찰")
    }
} 