package com.example.data.datasource.remote.friend

import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import kotlinx.coroutines.flow.Flow

/**
 * 친구 관련 원격 데이터 소스 인터페이스
 * 친구 목록 및 친구 요청 관련 Firebase Firestore 작업을 정의합니다.
 */
interface FriendRemoteDataSource {
    /**
     * 친구 목록 실시간 스트림을 가져옵니다.
     * @return 친구 목록의 Flow
     */
    fun getFriendsListStream(): Flow<List<Friend>>
    
    /**
     * 친구 목록을 Firestore에서 가져옵니다.
     * @return 작업 성공 여부
     */
    suspend fun fetchFriendsList(): Result<Unit>
    
    /**
     * 특정 친구와의 DM 채널 ID를 가져옵니다.
     * @param friendUserId 친구 사용자 ID
     * @return DM 채널 ID
     */
    suspend fun getDmChannelId(friendUserId: String): Result<String>
    
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
} 