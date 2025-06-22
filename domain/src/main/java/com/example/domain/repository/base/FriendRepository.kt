package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Friend
import com.example.domain.repository.DefaultRepository
import kotlinx.coroutines.flow.Flow

/**
 * 친구 관계 및 친구 요청 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface FriendRepository: DefaultRepository {
    /**
     * 현재 사용자의 친구 목록을 실시간 스트림으로 가져옵니다.
     * @param currentUserId 현재 사용자 ID
     * @return 친구 목록을 담은 Result Flow.
     */

    /**
     * 현재 사용자에게 온 친구 요청 목록을 실시간 스트림으로 가져옵니다.
     * @param currentUserId 현재 사용자 ID
     * @return 친구 요청 목록을 담은 Result Flow.
     */
    fun getFriendRequestsStream(): Flow<CustomResult<List<Friend>, Exception>>

}
