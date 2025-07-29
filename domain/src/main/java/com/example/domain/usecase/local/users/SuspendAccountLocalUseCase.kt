package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.LocalUserRepository
import javax.inject.Inject

interface SuspendAccountLocalUseCase {
    suspend operator fun invoke(): CustomResult<Unit, Exception>
}

class SuspendAccountLocalUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository
) : SuspendAccountLocalUseCase {

    override suspend operator fun invoke(): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 계정 일시 정지 처리")
    }
} 