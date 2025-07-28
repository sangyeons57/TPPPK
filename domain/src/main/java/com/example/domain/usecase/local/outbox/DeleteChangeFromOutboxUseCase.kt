package com.example.domain.usecase.local.outbox

import com.example.domain.repository.local.OutboxLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface DeleteChangeFromOutboxUseCase {
    suspend operator fun invoke(eventId: String): CustomResult<Unit, Exception>
}

class DeleteChangeFromOutboxUseCaseImpl @Inject constructor(
    private val outboxLocalRepository: OutboxLocalRepository
) : DeleteChangeFromOutboxUseCase {

    override suspend operator fun invoke(eventId: String): CustomResult<Unit, Exception> {
        return TODO("서버 동기화가 완료된 변경 사항을 Outbox에서 삭제")
    }
} 