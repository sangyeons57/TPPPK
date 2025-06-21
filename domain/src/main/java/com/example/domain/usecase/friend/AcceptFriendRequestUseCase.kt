package com.example.domain.usecase.friend

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.FriendRepository
import javax.inject.Inject

/**
 * 친구 요청을 수락하는 UseCase
 * 
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 */
class AcceptFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    /**
     * 특정 사용자의 친구 요청을 수락합니다.
     *
     * @param requesterId 수락할 친구 요청을 보낸 사용자의 ID.
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(requesterId: String, currentUserId: String): CustomResult<Unit, Exception> {
        return friendRepository.acceptFriendRequest(requesterId, currentUserId)
    }
} 