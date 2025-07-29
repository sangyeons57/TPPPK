package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.LocalUserRepository
import javax.inject.Inject

interface RemoveProfileImageLocalUseCase {
    suspend operator fun invoke(): CustomResult<Unit, Exception>
}

class RemoveProfileImageLocalUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository
) : RemoveProfileImageLocalUseCase {

    override suspend operator fun invoke(): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 프로필 이미지 제거")
    }
} 