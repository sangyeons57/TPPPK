package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 사용자의 정보를 가져오는 유스케이스 인터페이스
 */
interface GetUserUseCase {
    operator fun invoke(userId: String): Flow<CustomResult<User, Exception>>
}

/**
 * GetUserUseCase의 구현체
 * @param userRepository 사용자 데이터 접근을 위한 Repository
 */
class GetUserUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : GetUserUseCase {

    /**
     * 유스케이스를 실행하여 특정 사용자 정보를 가져옵니다.
     * @param userId 조회할 사용자 ID
     * @return Flow<Result<User>> 사용자 정보 로드 결과 Flow
     */
    override operator fun invoke(userId: String): Flow<CustomResult<User, Exception>> {
        return userRepository.observe(userId)
    }
} 