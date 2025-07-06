package com.example.domain.usecase.friend

import com.example.core_common.result.CustomResult
import com.example.core_common.result.exceptionOrNull
import com.example.core_common.result.getOrNull
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.FriendRepository
import javax.inject.Inject

/**
 * 친구 요청을 거절하는 UseCase
 * 
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class RejectFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 특정 사용자의 친구 요청을 거절합니다.
     *
     * @param friendRequestId 거절할 친구 요청 ID
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(friendRequestId: String): CustomResult<Unit, Exception> {
        return try {
            val currentUserResult = authRepository.getCurrentUserSession()
            if (currentUserResult.isFailure) {
                return CustomResult.Failure(currentUserResult.exceptionOrNull() ?: Exception("User not authenticated"))
            }
            
            val currentUser = currentUserResult.getOrNull()
            if (currentUser == null) {
                return CustomResult.Failure(Exception("User not authenticated"))
            }
            
            friendRepository.declineFriendRequest(currentUser.userId.value, friendRequestId)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}