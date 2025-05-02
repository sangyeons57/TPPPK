// 경로: domain/repository/FriendRepository.kt
package com.example.domain.repository

import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import kotlinx.coroutines.flow.Flow
import kotlin.Result

interface FriendRepository {

    // --- 친구 목록 관련 (FriendViewModel) ---
    /** 친구 목록 실시간 스트림 */
    fun getFriendsListStream(): Flow<List<Friend>>
    /** 친구 목록 새로고침 (API 호출 등) */
    suspend fun fetchFriendsList(): Result<Unit>
    /** 친구와의 DM 채널 ID 가져오기 */
    suspend fun getDmChannelId(friendUserId: String): Result<String>

    // --- 친구 추가 관련 (AddFriendViewModel) ---
    /** 사용자 이름으로 친구 요청 보내기 */
    suspend fun sendFriendRequest(username: String): Result<String> // 성공 시 메시지 반환 가정

    // --- 친구 요청 관리 관련 (AcceptFriendsViewModel) ---
    /** 받은 친구 요청 목록 가져오기 */
    suspend fun getFriendRequests(): Result<List<FriendRequest>>
    /** 친구 요청 수락 */
    suspend fun acceptFriendRequest(userId: String): Result<Unit>
    /** 친구 요청 거절 */
    suspend fun denyFriendRequest(userId: String): Result<Unit>

    // TODO: 친구 삭제 등 다른 기능 추가 가능
}