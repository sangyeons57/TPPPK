package com.example.domain.usecase.local.messages

import com.example.domain.repository.local.MessageLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface DeleteMessageUseCase {
    suspend operator fun invoke(messageId: String): CustomResult<Unit, Exception>
}

class DeleteMessageUseCaseImpl @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository
) : DeleteMessageUseCase {

    override suspend operator fun invoke(messageId: String): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에서 특정 메시지를 삭제")
    }
} 