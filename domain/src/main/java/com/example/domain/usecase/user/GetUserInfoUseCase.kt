package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 사용자 ID로 사용자 정보를 가져오는 유스케이스 인터페이스입니다.
 */
interface GetUserInfoUseCase {
    /**
     * 지정된 사용자 ID에 해당하는 사용자 정보를 반환합니다.
     *
     * @param userId 정보를 가져올 사용자의 ID
     * @return Flow<CustomResult<User, Exception>> 사용자 정보를 포함한 Flow
     */
    operator fun invoke(userId: String): Flow<CustomResult<User, Exception>>
}

/**
 * 특정 사용자 ID로 사용자 정보를 가져오는 유스케이스 구현체입니다.
 *
 * @property userRepository 사용자 관련 데이터를 제공하는 리포지토리
 */
class GetUserInfoUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : GetUserInfoUseCase {
    /**
     * 지정된 사용자 ID에 해당하는 사용자 정보를 반환합니다.
     *
     * @param userId 정보를 가져올 사용자의 ID
     * @return Flow<CustomResult<User, Exception>> 사용자 정보를 포함한 Flow
     */
    override operator fun invoke(userId: String): Flow<CustomResult<User, Exception>> {
        return userRepository.observe(userId)
    }
} 