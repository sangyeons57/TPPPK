package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.LocalUserRepository
import javax.inject.Inject

interface DeleteUserUseCase {
    suspend operator fun invoke(userId: String): CustomResult<Unit, Exception>
}

class DeleteUserUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository
) : DeleteUserUseCase {

    override suspend operator fun invoke(userId: String): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에서 특정 사용자를 삭제")
    }
} 