package com.example.domain.usecase.local.users

import com.example.domain.model.User
import com.example.domain.repository.local.UserLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface InsertUserUseCase {
    suspend operator fun invoke(user: User): CustomResult<Unit, Exception>
}

class InsertUserUseCaseImpl @Inject constructor(
    private val userLocalRepository: UserLocalRepository
) : InsertUserUseCase {

    override suspend operator fun invoke(user: User): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에 사용자 정보를 삽입하거나 갱신")
    }
} 