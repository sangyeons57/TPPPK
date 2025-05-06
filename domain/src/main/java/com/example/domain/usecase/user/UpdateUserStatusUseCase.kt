package com.example.domain.usecase.user

import com.example.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자 상태 메시지 업데이트 유스케이스 인터페이스
 */
interface UpdateUserStatusUseCase {
    suspend operator fun invoke(newStatus: String): Result<Unit>
}

/**
 * UpdateUserStatusUseCase의 구현체
 * @param userRepository 사용자 데이터 접근을 위한 Repository
 */
class UpdateUserStatusUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : UpdateUserStatusUseCase {

    /**
     * 유스케이스를 실행하여 사용자 상태 메시지를 업데이트합니다.
     * @param newStatus 새로운 상태 메시지
     * @return Result<Unit> 업데이트 처리 결과
     */
    override suspend fun invoke(newStatus: String): Result<Unit> {
        // TODO: UserRepository에 updateStatusMessage(newStatus) 함수 구현 필요
        // 해당 함수가 없다면 updateUserMemo 사용 고려
        // return userRepository.updateStatusMessage(newStatus)
        return userRepository.updateUserMemo(newStatus) // Assuming updateUserMemo serves this purpose
        
        // Remove temporary delay
        // kotlin.coroutines.delay(300)
        // println("UseCase: 상태 메시지 업데이트 성공 처리 (임시) - $newStatus")
        // return Result.success(Unit)
    }
} 