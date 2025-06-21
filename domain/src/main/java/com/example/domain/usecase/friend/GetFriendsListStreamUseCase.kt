package com.example.domain.usecase.friend

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Friend
import com.example.domain.repository.base.FriendRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 친구 목록 스트림을 가져오는 UseCase
 *
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 */
class GetFriendsListStreamUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    /**
     * 친구 목록 정보를 실시간 스트림으로 가져옵니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @return Flow<CustomResult<List<Friend>, Exception>> 친구 목록 정보 Flow
     */
    operator fun invoke(currentUserId: String): Flow<CustomResult<List<Friend>, Exception>> {
        return friendRepository.getFriendsStream(currentUserId)
    }
}