package com.example.domain.usecase.local.messages

import com.example.domain.model.Message
import com.example.domain.repository.local.MessageLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetMessagesByUserUseCase {
    operator fun invoke(userId: String): Flow<List<Message>>
}

class GetMessagesByUserUseCaseImpl @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository
) : GetMessagesByUserUseCase {

    override operator fun invoke(userId: String): Flow<List<Message>> {
        return TODO("특정 사용자가 작성한 모든 메시지 목록을 실시간으로 관찰")
    }
} 