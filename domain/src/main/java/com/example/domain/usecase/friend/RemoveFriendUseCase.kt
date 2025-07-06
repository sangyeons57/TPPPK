package com.example.domain.usecase.friend

import com.example.core_common.result.CustomResult
import com.example.core_common.result.exceptionOrNull
import com.example.core_common.result.getOrNull
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.FriendRepository
import javax.inject.Inject

/**
 * 친구 관계를 제거하는 UseCase
 * 
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class RemoveFriendUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 특정 사용자와의 친구 관계를 제거합니다.
     *
     * @param friendUserId 제거할 친구의 사용자 ID
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(friendUserId: String): CustomResult<Unit, Exception> {
        return try {
            val currentUserResult = authRepository.getCurrentUserSession()
            if (currentUserResult.isFailure) {
                return CustomResult.Failure(currentUserResult.exceptionOrNull() ?: Exception("User not authenticated"))
            }
            
            val currentUser = currentUserResult.getOrNull()
            if (currentUser == null) {
                return CustomResult.Failure(Exception("User not authenticated"))
            }
            
            friendRepository.removeFriend(currentUser.userId.value, friendUserId)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}