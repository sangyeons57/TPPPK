package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.UserRepository
import javax.inject.Inject

interface GetUserByIdUseCase {
    suspend operator fun invoke(userId: DocumentId): CustomResult<User, Exception>
}

class GetUserByIdUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : GetUserByIdUseCase {
    override suspend operator fun invoke(userId: DocumentId): CustomResult<User, Exception> {
        return when (val result = userRepository.findById(userId)) {
            is CustomResult.Success -> CustomResult.Success(result.data as User)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
} 