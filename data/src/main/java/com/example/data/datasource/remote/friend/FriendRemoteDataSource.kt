package com.example.data.datasource.remote.friend

import com.example.data.model.remote.friend.FriendRelationshipDto
import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 친구 관련 원격 데이터 소스 인터페이스
 * 친구 목록 및 친구 요청 관련 Firebase Firestore 작업을 정의합니다.
 */
interface FriendRemoteDataSource {
    /**
     * 친구 목록 실시간 스트림을 가져옵니다.
     * @return 친구 목록의 Flow
     */
    fun getFriendsStream(): Flow<Friend>
    
    /**
     * 친구 목록을 Firestore에서 가져옵니다.
     * @return 작업 성공 여부
     */
    suspend fun fetchFriendsList(): Result<Unit>
    
    /**
     * 사용자 이름으로 친구 요청을 보냅니다.
     * @param username 친구 요청을 보낼 사용자 이름
     * @return 성공 시 메시지 또는 실패 시 예외
     */
    suspend fun sendFriendRequest(username: String): Result<String>
    
    /**
     * 받은 친구 요청 목록을 가져옵니다.
     * @return 친구 요청 목록
     */
    suspend fun getFriendRequests(): Result<List<FriendRequest>>
    
    /**
     * 친구 요청을 수락합니다.
     * @param userId 수락할 친구 요청의 사용자 ID
     * @return 작업 성공 여부
     */
    suspend fun acceptFriendRequest(userId: String): Result<Unit>
    
    /**
     * 친구 요청을 거절합니다.
     * @param userId 거절할 친구 요청의 사용자 ID
     * @return 작업 성공 여부
     */
    suspend fun denyFriendRequest(userId: String): Result<Unit>

    /**
     * 특정 사용자의 모든 친구 관계 목록에 대한 실시간 리스너를 등록하고 Flow로 제공합니다.
     * @param currentUserId 현재 로그인한 사용자의 ID.
     * @return 친구 관계 문서 ID와 DTO 쌍의 리스트를 방출하는 Flow. Result로 감싸서 오류 처리.
     */
    fun getFriendRelationshipsStream(currentUserId: String): Flow<Result<List<Pair<String, FriendRelationshipDto>>>>

    /**
     * 친구 요청을 전송합니다.
     * - 요청자 측: users/{currentUserId}/friends/{targetUserId} 문서에 status: "pending_sent" 등으로 저장.
     * - 대상자 측: users/{targetUserId}/friends/{currentUserId} 문서에 status: "pending_received" 등으로 저장.
     * @param currentUserId 요청을 보내는 사용자의 ID.
     * @param targetUserId 요청을 받는 사용자의 ID.
     * @return Result<Unit> 성공 또는 실패.
     */
    suspend fun sendFriendRequest(currentUserId: String, targetUserId: String): Result<Unit>

    /**
     * 친구 요청을 수락합니다.
     * - 양측의 users/.../friends/{otherUserId} 문서의 status를 "accepted"로, acceptedAt을 현재 시간으로 업데이트.
     * @param currentUserId 요청을 수락하는 사용자(원래 요청을 받았던 사람)의 ID.
     * @param requesterId 친구 요청을 보냈던 사람의 ID.
     * @return Result<Unit> 성공 또는 실패.
     */
    suspend fun acceptFriendRequest(currentUserId: String, requesterId: String): Result<Unit>

    /**
     * 친구 관계를 삭제하거나 친구 요청을 거절합니다.
     * - 양측의 users/.../friends/{otherUserId} 문서를 삭제합니다.
     * @param currentUserId 작업을 수행하는 사용자의 ID.
     * @param friendId 삭제/거절할 상대방의 ID.
     * @return Result<Unit> 성공 또는 실패.
     */
    suspend fun removeOrDenyFriend(currentUserId: String, friendId: String): Result<Unit>
} 