package com.example.domain.usecase.local.messages

import com.example.domain.model.Message
import com.example.domain.repository.local.MessageLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface InsertMessageUseCase {
    suspend operator fun invoke(message: Message): CustomResult<Unit, Exception>
}

class InsertMessageUseCaseImpl @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository
) : InsertMessageUseCase {

    override suspend operator fun invoke(message: Message): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에 메시지를 삽입")
    }
} 