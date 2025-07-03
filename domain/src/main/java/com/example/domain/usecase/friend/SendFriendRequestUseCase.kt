package com.example.domain.usecase.friend

import com.example.core_common.result.CustomResult
import com.example.core_common.result.exceptionOrNull
import com.example.core_common.result.getOrNull
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.FriendRepository
import javax.inject.Inject

interface SendFriendRequestUseCase {
    suspend operator fun invoke(targetUsername: String): CustomResult<Unit, Exception>
}
/**
 * 친구 요청을 보내는 UseCase
 * 
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 */
class SendFriendRequestUseCaseImpl @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
): SendFriendRequestUseCase {
    /**
     * 특정 사용자에게 친구 요청을 보냅니다.
     *
     * @param targetUsername 친구 요청을 보낼 대상 사용자의 이름.
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result.
     */
    override suspend operator fun invoke(targetUsername: String): CustomResult<Unit, Exception> {
        return try {
            val currentUserResult = authRepository.getCurrentUserSession()
            if (currentUserResult.isFailure) {
                return CustomResult.Failure(currentUserResult.exceptionOrNull() ?: Exception("User not authenticated"))
            }
            
            val currentUser = currentUserResult.getOrNull()
            if (currentUser == null) {
                return CustomResult.Failure(Exception("User not authenticated"))
            }
            
            friendRepository.sendFriendRequest(currentUser.userId.value, targetUsername)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
} 