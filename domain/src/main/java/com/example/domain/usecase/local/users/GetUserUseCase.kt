package com.example.domain.usecase.local.users

import com.example.domain.model.base.User
import com.example.domain.repository.local.LocalUserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetUserUseCase {
    operator fun invoke(userId: String): Flow<User?>
}

class GetUserUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository
) : GetUserUseCase {

    override operator fun invoke(userId: String): Flow<User?> {
        return TODO("특정 사용자 정보를 실시간으로 관찰")
    }
} 