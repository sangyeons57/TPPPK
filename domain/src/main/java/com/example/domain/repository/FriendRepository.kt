package com.example.domain.repository

import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import com.example.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 친구 관계 및 친구 요청 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface FriendRepository {
    /**
     * 현재 사용자의 친구 목록을 실시간 스트림으로 가져옵니다.
     * @param currentUserId 현재 사용자 ID
     * @return 친구 목록을 담은 Result Flow.
     */
    fun getFriendsStream(currentUserId: String): Flow<Result<List<Friend>>>

    /**
     * 현재 사용자에게 온 친구 요청 목록을 실시간 스트림으로 가져옵니다.
     * @param currentUserId 현재 사용자 ID
     * @return 친구 요청 목록을 담은 Result Flow.
     */
    fun getFriendRequestsStream(currentUserId: String): Flow<Result<List<FriendRequest>>>

    /**
     * 친구 요청을 보냅니다.
     * @param senderId 요청을 보내는 사용자 ID
     * @param receiverNickname 요청을 받을 사용자의 닉네임 (또는 ID)
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun sendFriendRequest(senderId: String, receiverNickname: String): Result<Unit>

    /**
     * 친구 요청을 수락합니다.
     * @param friendRequestId 수락할 친구 요청 ID
     * @param currentUserId 현재 사용자 ID (요청을 받은 사람)
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun acceptFriendRequest(friendRequestId: String, currentUserId: String): Result<Unit>

    /**
     * 친구 요청을 거절합니다.
     * @param friendRequestId 거절할 친구 요청 ID
     * @param currentUserId 현재 사용자 ID (요청을 받은 사람)
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun declineFriendRequest(friendRequestId: String, currentUserId: String): Result<Unit>

    /**
     * 친구를 삭제합니다.
     * @param currentUserId 현재 사용자 ID
     * @param friendId 삭제할 친구의 사용자 ID
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun removeFriend(currentUserId: String, friendId: String): Result<Unit>

    /**
     * 사용자를 닉네임으로 검색합니다 (친구 추가 시 사용).
     * @param nickname 검색할 닉네임
     * @param currentUserId 현재 사용자 ID (검색 결과에서 자신 및 이미 친구인 사람 제외용)
     * @return 검색된 사용자 목록을 담은 Result. (User 모델 사용)
     */
    suspend fun searchUsersForFriend(nickname: String, currentUserId: String): Result<List<User>>
}
