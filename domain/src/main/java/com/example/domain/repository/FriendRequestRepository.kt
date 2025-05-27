package com.example.domain._repository

import com.example.domain.model.FriendRequest
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 친구 요청의 조회 및 관리에 특화된 데이터 처리를 위한 인터페이스입니다.
 */
interface FriendRequestRepository {
    /**
     * 현재 사용자가 보낸 친구 요청 목록을 실시간 스트림으로 가져옵니다.
     * @param currentUserId 현재 사용자(요청을 보낸 사람) ID
     * @return 보낸 친구 요청 목록을 담은 Result Flow.
     */
    fun getSentFriendRequestsStream(currentUserId: String): Flow<Result<List<FriendRequest>>>

    /**
     * 보낸 친구 요청을 취소합니다.
     * @param requestId 취소할 친구 요청 ID
     * @param currentUserId 현재 사용자(요청을 보낸 사람) ID (권한 확인용)
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun cancelFriendRequest(requestId: String, currentUserId: String): Result<Unit>

    /**
     * 특정 ID를 가진 친구 요청의 상세 정보를 가져옵니다.
     * @param requestId 친구 요청 ID
     * @return 해당 친구 요청 정보를 담은 Result.
     */
    suspend fun getFriendRequestDetails(requestId: String): Result<FriendRequest>
}
