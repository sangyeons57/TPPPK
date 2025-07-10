package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 사용자의 updatedAt 필드 변경을 실시간으로 관찰하는 UseCase 인터페이스입니다.
 */
interface ObserveUserUpdatedAtUseCase {
    /**
     * 특정 사용자의 updatedAt 필드를 실시간 Flow로 반환합니다.
     * @param userId 관찰할 사용자 ID
     * @return updatedAt 타임스탬프 Flow
     */
    operator fun invoke(userId: String): Flow<CustomResult<Long, Exception>>
}

/**
 * 특정 사용자의 updatedAt 필드 변경을 실시간으로 관찰하는 UseCase 구현체입니다.
 */
class ObserveUserUpdatedAtUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : ObserveUserUpdatedAtUseCase {
    
    /**
     * 특정 사용자의 updatedAt 필드를 실시간 Flow로 반환합니다.
     * @param userId 관찰할 사용자 ID
     * @return updatedAt 타임스탬프 Flow
     */
    override operator fun invoke(userId: String): Flow<CustomResult<Long, Exception>> {
        return userRepository.observeUserUpdatedAt(userId)
    }
} 