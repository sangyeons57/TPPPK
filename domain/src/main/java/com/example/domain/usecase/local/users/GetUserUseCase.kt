package com.example.domain.usecase.local.users

import com.example.domain.model.User
import com.example.domain.repository.local.UserLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetUserUseCase {
    operator fun invoke(userId: String): Flow<User?>
}

class GetUserUseCaseImpl @Inject constructor(
    private val userLocalRepository: UserLocalRepository
) : GetUserUseCase {

    override operator fun invoke(userId: String): Flow<User?> {
        return TODO("특정 사용자 정보를 실시간으로 관찰")
    }
} 