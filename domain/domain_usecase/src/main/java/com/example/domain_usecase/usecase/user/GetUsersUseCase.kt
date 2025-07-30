package com.example.domain_usecase.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain_repository.base.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 여러 사용자 ID를 기반으로 사용자 정보 목록을 가져오는 유스케이스
 */
interface GetUsersUseCase {
    operator fun invoke(userIds: List<String>): Flow<CustomResult<List<User>, Exception>>
}

class GetUsersUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : GetUsersUseCase {
    override operator fun invoke(userIds: List<String>): Flow<CustomResult<List<User>, Exception>> {
        if (userIds.isEmpty()) {
            return flowOf(CustomResult.Success(emptyList()))
        }
        return userRepository.observeUsers(userIds)
    }
}
