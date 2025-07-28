package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.UserLocalRepository
import javax.inject.Inject

interface UpdateUserMemoLocalUseCase {
    suspend operator fun invoke(memo: String): CustomResult<Unit, Exception>
}

class UpdateUserMemoLocalUseCaseImpl @Inject constructor(
    private val userLocalRepository: UserLocalRepository
) : UpdateUserMemoLocalUseCase {

    override suspend operator fun invoke(memo: String): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 사용자 메모 업데이트")
    }
} 