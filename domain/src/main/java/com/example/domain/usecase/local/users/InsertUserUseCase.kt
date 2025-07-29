package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.local.LocalUserRepository
import javax.inject.Inject

interface InsertUserUseCase {
    suspend operator fun invoke(user: User): CustomResult<Unit, Exception>
}

class InsertUserUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository
) : InsertUserUseCase {

    override suspend operator fun invoke(user: User): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에 사용자 정보를 삽입하거나 갱신")
    }
} 