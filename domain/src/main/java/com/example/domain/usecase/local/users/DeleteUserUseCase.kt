package com.example.domain.usecase.local.users

import com.example.domain.repository.local.UserLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface DeleteUserUseCase {
    suspend operator fun invoke(userId: String): CustomResult<Unit, Exception>
}

class DeleteUserUseCaseImpl @Inject constructor(
    private val userLocalRepository: UserLocalRepository
) : DeleteUserUseCase {

    override suspend operator fun invoke(userId: String): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에서 특정 사용자를 삭제")
    }
} 