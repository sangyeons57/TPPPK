package com.example.domain.usecase.local.outbox

import com.example.domain.model.OutboxEvent
import com.example.domain.repository.local.OutboxLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetPendingChangesUseCase {
    operator fun invoke(): Flow<List<OutboxEvent>>
}

class GetPendingChangesUseCaseImpl @Inject constructor(
    private val outboxLocalRepository: OutboxLocalRepository
) : GetPendingChangesUseCase {

    override operator fun invoke(): Flow<List<OutboxEvent>> {
        return TODO("서버에 아직 동기화되지 않은 모든 변경 사항 목록을 관찰")
    }
} 