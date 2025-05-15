// 경로: domain/repository/FriendRepository.kt
package com.example.domain.repository

import com.example.domain.model.FriendRelationship
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 친구 관계 데이터 처리를 위한 Repository 인터페이스입니다.
 * Firestore의 users/{userId}/friends 서브컬렉션을 주로 다룹니다.
 */
interface FriendRepository {

    /**
     * 현재 사용자의 모든 친구 관계 목록을 실시간 스트림으로 가져옵니다.
     * Firestore의 users/{currentUserId}/friends 컬렉션에 해당합니다.
     * @return Result<Flow<List<FriendRelationship>>> 친구 관계 목록 스트림.
     */
    fun getFriendRelationshipsStream(): Flow<Result<List<FriendRelationship>>>

    /**
     * 친구 관계 목록을 서버에서 새로고침 (one-time fetch) 합니다.
     * 필요에 따라 내부적으로 getFriendRelationshipsStream에 데이터를 푸시할 수 있습니다.
     * @return Result<Unit> 성공 또는 실패.
     */
    suspend fun refreshFriendRelationships(): Result<Unit>

    /**
     * 특정 사용자에게 친구 요청을 보냅니다.
     * 대상 사용자의 friends 서브컬렉션에는 status: pending_received,
     * 요청자의 friends 서브컬렉션에는 status: pending_sent로 문서를 생성/업데이트합니다.
     * @param targetUserId 친구 요청을 보낼 대상의 사용자 ID.
     * @return Result<Unit> 성공 또는 실패.
     */
    suspend fun sendFriendRequest(targetUserId: String): Result<Unit>

    /**
     * 받은 친구 요청을 수락합니다.
     * 요청자와 대상자 모두의 friends 서브컬렉션에서 해당 문서의 status를 'accepted'로 변경하고 acceptedAt을 설정합니다.
     * @param requesterId 친구 요청을 보낸 사용자의 ID.
     * @return Result<Unit> 성공 또는 실패.
     */
    suspend fun acceptFriendRequest(requesterId: String): Result<Unit>

    /**
     * 받은 친구 요청을 거절하거나 기존 친구 관계를 삭제합니다.
     * 요청자와 대상자 모두의 friends 서브컬렉션에서 해당 문서를 삭제합니다.
     * @param friendId 거절할 요청의 사용자 ID 또는 삭제할 친구의 사용자 ID.
     * @return Result<Unit> 성공 또는 실패.
     */
    suspend fun removeOrDenyFriend(friendId: String): Result<Unit>

    /** 
     * 친구와의 DM 채널 ID 가져오기
     * 채널 정보는 이제 채널 시스템으로 통합되어 관리됩니다.
     * 채널 타입이 'DM'인 채널을 사용합니다.
     * @param friendUserId 친구의 사용자 ID
     * @return 채널 ID 또는 오류
     */
    suspend fun getDmChannelId(friendUserId: String): Result<String>
}