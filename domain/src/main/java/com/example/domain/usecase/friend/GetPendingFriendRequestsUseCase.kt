package com.example.domain.usecase.friend

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.base.Friend
import com.example.domain.repository.FriendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 받은 친구 요청 목록(Friend) 스트림을 가져오는 UseCase.
 * 내부적으로 전체 친구 목록 스트림을 필터링합니다.
 *
 * @property friendRepository 친구 관련 기능을 제공하는 Repository.
 */
class GetPendingFriendRequestsUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    /**
     * 받은 친구 요청 목록을 실시간 스트림으로 가져옵니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @return Flow<CustomResult<List<Friend>, Exception>> 받은 친구 요청 목록 스트림.
     */
    operator fun invoke(currentUserId: String): Flow<CustomResult<List<Friend>, Exception>> {
        return friendRepository.getFriendsStream(currentUserId).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    // PENDING 상태인 친구 요청만 필터링
                    val pendingRequests = result.data.filter { it.status == FriendStatus.PENDING }
                    CustomResult.Success(pendingRequests)
                }
                is CustomResult.Failure -> {
                    result
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error in GetPendingFriendRequestsUseCase"))
                }
            }
        }
    }
}