package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

interface GetUserStreamUseCase {
    suspend operator fun invoke(userId: String): Flow<CustomResult<User, Exception>>
}

class GetUserStreamUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : GetUserStreamUseCase {
    override suspend operator fun invoke(userId: String): Flow<CustomResult<User, Exception>> {
        return when (val result = userRepository.observe(DocumentId.from(userId)).first()) {
            is CustomResult.Success -> flowOf( CustomResult.Success((result.data as User)) )
            is CustomResult.Failure -> flowOf( CustomResult.Failure(Exception("로그인이 필요합니다.")) )
            is CustomResult.Initial -> flowOf( CustomResult.Initial )
            is CustomResult.Loading -> flowOf( CustomResult.Loading )
            is CustomResult.Progress -> flowOf( CustomResult.Progress(result.progress) )
        }
    }
} 