package com.example.domain.usecase.local.outbox

import com.example.domain.model.OutboxEvent
import com.example.domain.repository.local.OutboxLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface AddChangeToOutboxUseCase {
    suspend operator fun invoke(change: OutboxEvent): CustomResult<Unit, Exception>
}

class AddChangeToOutboxUseCaseImpl @Inject constructor(
    private val outboxLocalRepository: OutboxLocalRepository
) : AddChangeToOutboxUseCase {

    override suspend operator fun invoke(change: OutboxEvent): CustomResult<Unit, Exception> {
        return try {
            outboxLocalRepository.addChange(change)
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Error(e)
        }
    }
}