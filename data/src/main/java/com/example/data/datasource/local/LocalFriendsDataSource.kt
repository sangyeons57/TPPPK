package com.example.data.datasource.local

import com.example.domain.model.base.Friend
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 로컬 친구 데이터 저장소 인터페이스
 * 3-tier 클라이언트 주도 동기화 아키텍처를 지원합니다
 */
interface LocalFriendsDataSource {

    // === 기본 CRUD 작업 ===

    /**
     * 친구 관계 ID로 단일 친구 관계 조회
     * @param friendshipId 친구 관계 ID
     * @return 친구 관계 정보 (없으면 null)
     */
    suspend fun getFriendshipById(friendshipId: String): Friend?

    /**
     * 사용자별 친구 목록 조회
     * @param userId 사용자 ID
     * @return 친구 목록
     */
    suspend fun getFriendsByUser(userId: String): List<Friend>

    /**
     * 두 사용자 간의 친구 관계 조회
     * @param user1Id 첫 번째 사용자 ID
     * @param user2Id 두 번째 사용자 ID
     * @return 친구 관계 정보 (없으면 null)
     */
    suspend fun getFriendshipBetweenUsers(user1Id: String, user2Id: String): Friend?

    /**
     * 친구 상태별 친구 목록 조회
     * @param userId 사용자 ID
     * @param status 친구 상태 (PENDING, ACCEPTED, BLOCKED)
     * @return 친구 목록
     */
    suspend fun getFriendsByStatus(userId: String, status: String): List<Friend>

    /**
     * 대기 중인 친구 요청 목록 조회
     * @param userId 사용자 ID
     * @return 대기 중인 친구 요청 목록
     */
    suspend fun getPendingFriendRequests(userId: String): List<Friend>

    /**
     * 수락된 친구 요청 목록 조회
     * @param userId 사용자 ID
     * @return 수락된 친구 요청 목록
     */
    suspend fun getReceivedFriendRequests(userId: String): List<Friend>

    /**
     * 온라인 친구 목록 조회
     * @param userId 사용자 ID
     * @return 온라인 친구 목록
     */
    suspend fun getOnlineFriends(userId: String): List<Friend>

    /**
     * 모든 친구 관계 목록 조회
     * @return 친구 관계 목록
     */
    suspend fun getAllFriendships(): List<Friend>

    /**
     * 단일 친구 관계 저장
     * @param friend 저장할 친구 관계
     */
    suspend fun saveFriendship(friend: Friend)

    /**
     * 친구 관계 목록 배치 저장
     * @param friends 저장할 친구 관계 목록
     */
    suspend fun saveFriendships(friends: List<Friend>)

    /**
     * 친구 관계 삭제
     * @param friendshipId 삭제할 친구 관계 ID
     */
    suspend fun deleteFriendship(friendshipId: String)

    /**
     * 사용자별 친구 관계 일괄 삭제
     * @param userId 사용자 ID
     */
    suspend fun deleteFriendshipsByUser(userId: String)

    /**
     * 친구 상태 업데이트
     * @param friendshipId 친구 관계 ID
     * @param status 새로운 상태
     */
    suspend fun updateFriendshipStatus(friendshipId: String, status: String)

    // === 3-tier 동기화 지원 ===

    /**
     * 특정 시점 이후 업데이트된 친구 관계들을 조회 (증분 동기화용)
     * @param timestamp 기준 시간
     * @return 업데이트된 친구 관계 목록
     */
    suspend fun getFriendshipsUpdatedAfter(timestamp: Instant): List<Friend>

    /**
     * 사용자별 친구 관계 실시간 관찰
     * @param userId 사용자 ID
     * @return 친구 관계 목록 Flow
     */
    fun observeFriendsByUser(userId: String): Flow<List<Friend>>

    /**
     * 특정 친구 관계 실시간 관찰
     * @param friendshipId 친구 관계 ID
     * @return 친구 관계 정보 Flow
     */
    fun observeFriendshipById(friendshipId: String): Flow<Friend?>

    /**
     * 친구 상태별 실시간 관찰
     * @param userId 사용자 ID
     * @param status 친구 상태
     * @return 친구 목록 Flow
     */
    fun observeFriendsByStatus(userId: String, status: String): Flow<List<Friend>>

    /**
     * 대기 중인 친구 요청 실시간 관찰
     * @param userId 사용자 ID
     * @return 대기 중인 친구 요청 목록 Flow
     */
    fun observePendingFriendRequests(userId: String): Flow<List<Friend>>

    // === Outbox 관리 ===

    /**
     * 친구 관계 변경사항을 Outbox에 기록
     * @param friendshipId 친구 관계 ID
     * @param operation 작업 유형 (CREATE, UPDATE, DELETE)
     * @param payload 변경 데이터 (선택적)
     */
    suspend fun addToOutbox(friendshipId: String, operation: String, payload: String? = null)

    /**
     * 대기 중인 Outbox 작업 목록 조회
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingOutboxOperations(): List<FriendOutboxOperation>

    /**
     * Outbox 작업 완료 처리
     * @param operationId 작업 ID
     */
    suspend fun markOutboxOperationComplete(operationId: String)

    /**
     * Outbox 작업 재시도 증가
     * @param operationId 작업 ID
     */
    suspend fun incrementOutboxRetries(operationId: String)

    // === 동기화 메타데이터 관리 ===

    /**
     * 마지막 동기화 커서 조회
     * @return 마지막 서버 커서 (밀리초)
     */
    suspend fun getLastSyncCursor(): Long?

    /**
     * 동기화 커서 업데이트
     * @param cursor 새로운 서버 커서
     * @param timestamp 동기화 시간
     */
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)

    // === 유틸리티 ===

    /**
     * 친구 관계 존재 여부 확인
     * @param friendshipId 친구 관계 ID
     * @return 존재 여부
     */
    suspend fun friendshipExists(friendshipId: String): Boolean

    /**
     * 두 사용자가 친구인지 확인
     * @param user1Id 첫 번째 사용자 ID
     * @param user2Id 두 번째 사용자 ID
     * @return 친구 여부
     */
    suspend fun areFriends(user1Id: String, user2Id: String): Boolean

    /**
     * 사용자별 친구 수 조회
     * @param userId 사용자 ID
     * @return 친구 수
     */
    suspend fun getFriendCount(userId: String): Int

    /**
     * 전체 친구 관계 수 조회
     * @return 전체 친구 관계 수
     */
    suspend fun getTotalFriendshipCount(): Int

    /**
     * 사용자별 대기 중인 친구 요청 수 조회
     * @param userId 사용자 ID
     * @return 대기 중인 요청 수
     */
    suspend fun getPendingFriendRequestCount(userId: String): Int

    /**
     * 온라인 친구 수 조회
     * @param userId 사용자 ID
     * @return 온라인 친구 수
     */
    suspend fun getOnlineFriendCount(userId: String): Int

    /**
     * 모든 친구 관계 데이터 삭제 (개발/테스트용)
     */
    suspend fun clearAllFriendships()
}

/**
 * 친구 관계 Outbox 작업 정보
 */
data class FriendOutboxOperation(
    val id: String,
    val friendshipId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)