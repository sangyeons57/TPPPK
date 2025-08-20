package com.example.domain_usecase.usecase.friend

import com.example.core_common.result.CustomResult
import com.example.core_common.result.exceptionOrNull
import com.example.core_common.result.getOrNull
import com.example.domain.vo.UserId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.FriendRepository
import javax.inject.Inject

/**
 * 친구 요청을 수락하는 UseCase
 * 
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class AcceptFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 특정 사용자의 친구 요청을 수락합니다.
     *
     * @param friendId 수락할 친구 요청을 보낸 사용자의 ID.
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(friendId: UserId): CustomResult<Unit, Exception> {
        return try {
            friendRepository.acceptFriendRequest(friendId)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
} 