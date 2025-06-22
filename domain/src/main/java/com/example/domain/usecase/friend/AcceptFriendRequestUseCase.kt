package com.example.domain.usecase.friend

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.Friend
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
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
    suspend operator fun invoke(requesterId: UserId, currentUserId: UserId): CustomResult<Unit, Exception> {
        return when (val friendResult = friendRepository.findById(DocumentId.from(requesterId))) {
            is CustomResult.Success -> {
                val friend = friendResult.data as Friend
                friend.acceptRequest()

                when (val saveResult = friendRepository.save(friend)){
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Progress -> CustomResult.Progress(saveResult.progress)
                    is CustomResult.Success -> {
                        EventDispatcher.publish(friend)
                        CustomResult.Success(Unit)
                    }
                    is CustomResult.Failure -> CustomResult.Failure(saveResult.error)
                }
            }
            is CustomResult.Failure -> CustomResult.Failure(friendResult.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(friendResult.progress)
        }
    }
} 