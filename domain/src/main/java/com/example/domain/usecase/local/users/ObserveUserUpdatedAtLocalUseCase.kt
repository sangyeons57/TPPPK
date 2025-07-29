package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.LocalUserRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

interface ObserveUserUpdatedAtLocalUseCase {
    operator fun invoke(userId: DocumentId): Flow<CustomResult<Instant, Exception>>
}

class ObserveUserUpdatedAtLocalUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository
) : ObserveUserUpdatedAtLocalUseCase {

    override operator fun invoke(userId: DocumentId): Flow<CustomResult<Instant, Exception>> {
        return TODO("로컬 저장소에서 특정 사용자의 마지막 업데이트 시간을 실시간으로 관찰")
    }
} 