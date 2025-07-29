package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.LocalUserRepository
import javax.inject.Inject

interface UpdateFcmTokenLocalUseCase {
    suspend operator fun invoke(token: String): CustomResult<Unit, Exception>
}

class UpdateFcmTokenLocalUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository
) : UpdateFcmTokenLocalUseCase {

    override suspend operator fun invoke(token: String): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 FCM 토큰 업데이트")
    }
} 