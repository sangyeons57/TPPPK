package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.UserLocalRepository
import javax.inject.Inject

interface UpdateUserStatusLocalUseCase {
    suspend operator fun invoke(status: String): CustomResult<Unit, Exception>
}

class UpdateUserStatusLocalUseCaseImpl @Inject constructor(
    private val userLocalRepository: UserLocalRepository
) : UpdateUserStatusLocalUseCase {

    override suspend operator fun invoke(status: String): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 사용자 상태 메시지 업데이트")
    }
} 