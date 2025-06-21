package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.UserStatus
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import javax.inject.Inject

/**
 * 사용자 상태 메시지 업데이트 유스케이스 인터페이스
 */
interface UpdateUserStatusUseCase {
    /**
     * Updates the current user's online status through domain behaviour and persists it.
     */
    suspend operator fun invoke(status: UserStatus): CustomResult<Unit, Exception>
}

/**
 * UpdateUserStatusUseCase의 구현체
 * @param userRepository 사용자 데이터 접근을 위한 Repository
 */
class UpdateUserStatusUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : UpdateUserStatusUseCase {

    /**
     * 유스케이스를 실행하여 사용자 상태 메시지를 업데이트합니다.
     * @param newStatus 새로운 상태 메시지
     * @return Result<Unit> 업데이트 처리 결과
     */
    override suspend fun invoke(status: UserStatus): CustomResult<Unit, Exception> {
        val sessionRes = authRepository.getCurrentUserSession()
        if (sessionRes !is CustomResult.Success) {
            return CustomResult.Failure(Exception("User not logged in"))
        }
        val userRes = userRepository.findById(sessionRes.data.userId)
        if (userRes is CustomResult.Failure) {
            return CustomResult.Failure(userRes.error)
        } else if (userRes !is CustomResult.Success) {
            return CustomResult.Failure(Exception("User not found"))
        }
        val user = userRes.data
        user.updateUserStatus(status)
        val saveRes = userRepository.save(user)
        return when (saveRes) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(saveRes.error)
            else -> CustomResult.Failure(Exception("Unknown error"))
        }
        // Remove temporary delay
        // kotlin.coroutines.delay(300)
        // println("UseCase: 상태 메시지 업데이트 성공 처리 (임시) - $newStatus")
        // return Result.success(Unit)
    }
} 