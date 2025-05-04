package com.example.data.datasource.local.friend

import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import kotlinx.coroutines.flow.Flow

/**
 * 친구 관련 로컬 데이터 소스 인터페이스
 * 친구 목록 및 친구 요청 관련 Room 데이터베이스 작업을 정의합니다.
 */
interface FriendLocalDataSource {
    /**
     * 친구 목록 스트림을 가져옵니다.
     * @return 친구 목록의 Flow
     */
    fun getFriendsStream(): Flow<List<Friend>>
    
    /**
     * 친구 목록을 로컬 데이터베이스에 저장합니다.
     * @param friends 저장할 친구 목록
     */
    suspend fun saveFriends(friends: List<Friend>)
    
    /**
     * 특정 친구 정보를 가져옵니다.
     * @param friendId 친구 ID
     * @return 친구 정보 또는 null
     */
    suspend fun getFriendById(friendId: String): Friend?
    
    /**
     * 특정 친구 정보를 저장합니다.
     * @param friend 저장할 친구 정보
     */
    suspend fun saveFriend(friend: Friend)
    
    /**
     * 특정 친구 정보를 삭제합니다.
     * @param friendId 삭제할 친구 ID
     */
    suspend fun deleteFriend(friendId: String)
    
    /**
     * 모든 친구 정보를 삭제합니다.
     */
    suspend fun deleteAllFriends()
    
    /**
     * 친구 요청 목록을 가져옵니다.
     * @return 친구 요청 목록
     */
    suspend fun getFriendRequests(): List<FriendRequest>
    
    /**
     * 친구 요청 목록을 저장합니다.
     * @param requests 저장할 친구 요청 목록
     */
    suspend fun saveFriendRequests(requests: List<FriendRequest>)
    
    /**
     * 친구 요청을 저장합니다.
     * @param request 저장할 친구 요청
     */
    suspend fun saveFriendRequest(request: FriendRequest)
    
    /**
     * 친구 요청을 삭제합니다.
     * @param userId 삭제할 요청의 사용자 ID
     */
    suspend fun deleteFriendRequest(userId: String)
    
    /**
     * 모든 친구 요청을 삭제합니다.
     */
    suspend fun deleteAllFriendRequests()
    
    /**
     * 친구 요청이 수락되었을 때 처리합니다.
     * @param userId 수락된 친구의 사용자 ID
     * @param friend 추가될 친구 정보
     */
    suspend fun acceptFriendRequest(userId: String, friend: Friend)
} 