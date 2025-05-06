package com.example.domain.usecase.friend

import com.example.domain.repository.FriendRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 친구 요청을 거절하거나 친구를 삭제하는 UseCase
 *
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 */
class RemoveOrDenyFriendUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    /**
     * 특정 사용자의 친구 요청을 거절하거나 친구 관계를 삭제합니다.
     *
     * @param friendId 거절할 친구 요청의 사용자 ID 또는 삭제할 친구의 ID.
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(friendId: String): Result<Unit> {
        return friendRepository.removeOrDenyFriend(friendId)
    }
} 