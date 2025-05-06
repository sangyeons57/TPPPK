package com.example.domain.usecase.friend

import com.example.domain.repository.FriendRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 친구 요청을 보내는 UseCase
 * 
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 */
class SendFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    /**
     * 특정 사용자에게 친구 요청을 보냅니다.
     *
     * @param targetUserId 친구 요청을 보낼 대상 사용자의 ID.
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result.
     */
    suspend operator fun invoke(targetUserId: String): Result<Unit> {
        return friendRepository.sendFriendRequest(targetUserId)
    }
} 