package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.UserStatus
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 사용자 상태 메시지 업데이트 유스케이스 인터페이스
 */
interface UpdateUserStatusUseCase {
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
        // TODO: UserRepository에 updateStatusMessage(newStatus) 함수 구현 필요
        // 해당 함수가 없다면 updateUserMemo 사용 고려
        val seesionResult = authRepository.getCurrentUserSession()
        return when (seesionResult) {
            is CustomResult.Success -> userRepository.updateUserConnectionStatus(seesionResult.data.userId, status)
            else -> CustomResult.Failure(Exception("Failed to get current user session"))
        }
        // Remove temporary delay
        // kotlin.coroutines.delay(300)
        // println("UseCase: 상태 메시지 업데이트 성공 처리 (임시) - $newStatus")
        // return Result.success(Unit)
    }
} 