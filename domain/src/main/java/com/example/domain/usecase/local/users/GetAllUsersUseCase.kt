package com.example.domain.usecase.local.users

import com.example.domain.model.base.User
import com.example.domain.repository.local.LocalUserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetAllUsersUseCase {
    operator fun invoke(): Flow<List<User>>
}

class GetAllUsersUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository
) : GetAllUsersUseCase {

    override operator fun invoke(): Flow<List<User>> {
        return TODO("로컬에 저장된 모든 사용자 목록을 실시간으로 관찰")
    }
} 