package com.example.domain.usecase.user

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자 정보를 업데이트하는 UseCase
 * 
 * @property userRepository 사용자 관련 기능을 제공하는 Repository
 */
class UpdateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 사용자 정보를 업데이트합니다.
     *
     * @param user 업데이트할 사용자 정보
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(user: User): Result<Unit> {
        val userWithTimestamp = user.copy(updatedAt = DateTimeUtil.nowInstant())
        return userRepository.updateUserProfile(userWithTimestamp)
    }
} 