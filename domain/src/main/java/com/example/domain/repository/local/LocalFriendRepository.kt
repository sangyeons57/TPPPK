package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Friend
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Friend Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (Remote FriendRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - Friend 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeFriendById
 * - observeAllEntities -> observeAllFriends
 * - observeEntityUpdatedAt -> observeFriendUpdatedAt
 * - getEntityById -> getFriendById
 * - getEntitiesByIds -> getFriendsByIds
 * - getAllEntities -> getAllFriends
 * - saveEntity -> saveFriend
 * - saveEntities -> saveFriends
 * - deleteEntity -> deleteFriend
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalFriendRepository : BaseLocalRepository<Friend> {

    // === BaseLocalRepository 메서드 (구현체에서 친구 전용 메서드로 매핑) ===
    // observeEntityById -> observeFriendById
    // observeAllEntities -> observeAllFriends  
    // observeEntityUpdatedAt -> observeFriendUpdatedAt
    // getEntityById -> getFriendById
    // getEntitiesByIds -> getFriendsByIds
    // getAllEntities -> getAllFriends
    // saveEntity -> saveFriend
    // saveEntities -> saveFriends
    // deleteEntity -> deleteFriend
    // getEntitiesUpdatedAfter -> getFriendsUpdatedAfter
    // clearAllEntities -> clearAllFriends
    // getTotalEntityCount -> getTotalFriendCount
    // entityExists -> friendExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 친구를 실시간 관찰
     * @param friendId 친구 ID
     * @return 친구 Flow (null 가능)
     */
    fun observeFriendById(friendId: String): Flow<Friend?>

    /**
     * 친구 이름으로 정확히 일치하는 친구를 실시간 관찰
     * @param name 친구 이름
     * @return 친구 Flow (null 가능)
     */
    fun observeFriendByName(name: UserName): Flow<Friend?>

    /**
     * 친구 이름으로 검색하는 친구 목록을 실시간 관찰
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 친구 목록 Flow
     */
    fun observeFriendsByName(name: String, limit: Int = 10): Flow<List<Friend>>

    /**
     * 상태별 친구 목록을 실시간 관찰
     * @param status 친구 상태
     * @return 친구 목록 Flow
     */
    fun observeFriendsByStatus(status: FriendStatus): Flow<List<Friend>>

    /**
     * 수락된 친구들을 실시간 관찰
     * @return 수락된 친구 목록 Flow
     */
    fun observeAcceptedFriends(): Flow<List<Friend>>

    /**
     * 보낸 친구 요청들을 실시간 관찰
     * @return 보낸 친구 요청 목록 Flow
     */
    fun observeSentFriendRequests(): Flow<List<Friend>>

    /**
     * 받은 친구 요청들을 실시간 관찰
     * @return 받은 친구 요청 목록 Flow
     */
    fun observeReceivedFriendRequests(): Flow<List<Friend>>

    /**
     * 차단된 친구들을 실시간 관찰
     * @return 차단된 친구 목록 Flow
     */
    fun observeBlockedFriends(): Flow<List<Friend>>

    /**
     * 모든 친구를 실시간 관찰
     * @return 전체 친구 목록 Flow
     */
    fun observeAllFriends(): Flow<List<Friend>>

    /**
     * 특정 친구의 updatedAt 필드 변경을 실시간 관찰
     * @param friendId 친구 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeFriendUpdatedAt(friendId: String): Flow<Long?>

    /**
     * 주어진 ID 목록에 해당하는 친구 목록을 실시간 관찰
     * @param friendIds 친구 ID 목록
     * @return 친구 목록 Flow
     */
    fun observeFriends(friendIds: List<String>): Flow<List<Friend>>

    // === 단순 읽기 작업 ===

    /**
     * 친구 ID로 조회
     * @param friendId 친구 ID
     * @return 친구 (없으면 null)
     */
    suspend fun getFriendById(friendId: String): Friend?

    /**
     * 친구 이름으로 조회 (정확히 일치)
     * @param name 친구 이름
     * @return 친구 (없으면 null)
     */
    suspend fun getFriendByName(name: UserName): Friend?

    /**
     * 친구 이름으로 검색 (부분 일치)
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 친구 목록
     */
    suspend fun searchFriendsByName(name: String, limit: Int = 10): List<Friend>

    /**
     * 상태별 친구 목록 조회
     * @param status 친구 상태
     * @return 친구 목록
     */
    suspend fun getFriendsByStatus(status: FriendStatus): List<Friend>

    /**
     * 수락된 친구들 조회
     * @return 수락된 친구 목록
     */
    suspend fun getAcceptedFriends(): List<Friend>

    /**
     * 보낸 친구 요청들 조회
     * @return 보낸 친구 요청 목록
     */
    suspend fun getSentFriendRequests(): List<Friend>

    /**
     * 받은 친구 요청들 조회
     * @return 받은 친구 요청 목록
     */
    suspend fun getReceivedFriendRequests(): List<Friend>

    /**
     * 차단된 친구들 조회
     * @return 차단된 친구 목록
     */
    suspend fun getBlockedFriends(): List<Friend>

    /**
     * 모든 친구 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 친구 목록
     */
    suspend fun getAllFriends(limit: Int? = null): List<Friend>

    /**
     * 여러 친구 ID로 조회
     * @param friendIds 친구 ID 목록
     * @return 친구 목록
     */
    suspend fun getFriendsByIds(friendIds: List<String>): List<Friend>

    /**
     * 특정 시간 이후 요청된 친구들 조회
     * @param timestamp 기준 시간
     * @return 친구 목록
     */
    suspend fun getFriendsRequestedAfter(timestamp: Instant): List<Friend>

    /**
     * 특정 시간 이후 수락된 친구들 조회
     * @param timestamp 기준 시간
     * @return 친구 목록
     */
    suspend fun getFriendsAcceptedAfter(timestamp: Instant): List<Friend>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 친구 저장 (생성/수정)
     * @param friend 저장할 친구
     * @return 성공 여부
     */
    suspend fun saveFriend(friend: Friend): CustomResult<Unit, Exception>

    /**
     * 친구 대량 저장 (동기화용)
     * @param friends 저장할 친구 목록
     * @return 성공 여부
     */
    suspend fun saveFriends(friends: List<Friend>): CustomResult<Unit, Exception>

    /**
     * 친구 삭제 (Soft Delete)
     * @param friendId 친구 ID
     * @return 성공 여부
     */
    suspend fun deleteFriend(friendId: String): CustomResult<Unit, Exception>

    /**
     * 친구 정보 업데이트 (로컬)
     * @param friendId 친구 ID
     * @param name 새로운 이름 (nullable)
     * @param profileImageUrl 새로운 프로필 이미지 URL (nullable)
     * @param status 새로운 상태 (nullable)
     * @return 성공 여부
     */
    suspend fun updateFriend(
        friendId: String,
        name: UserName? = null,
        profileImageUrl: ImageUrl? = null,
        status: FriendStatus? = null
    ): CustomResult<Unit, Exception>

    /**
     * 친구 요청 수락
     * @param friendId 친구 ID
     * @return 성공 여부
     */
    suspend fun acceptFriendRequest(friendId: String): CustomResult<Unit, Exception>

    /**
     * 친구 차단
     * @param friendId 친구 ID
     * @return 성공 여부
     */
    suspend fun blockFriend(friendId: String): CustomResult<Unit, Exception>

    /**
     * 친구 제거
     * @param friendId 친구 ID
     * @return 성공 여부
     */
    suspend fun removeFriend(friendId: String): CustomResult<Unit, Exception>

    /**
     * 친구 요청을 대기 중으로 변경
     * @param friendId 친구 ID
     * @return 성공 여부
     */
    suspend fun markFriendRequestAsPending(friendId: String): CustomResult<Unit, Exception>

    /**
     * 친구 요청을 요청됨으로 변경
     * @param friendId 친구 ID
     * @return 성공 여부
     */
    suspend fun markFriendRequestAsRequested(friendId: String): CustomResult<Unit, Exception>

    /**
     * 친구 이름 변경
     * @param friendId 친구 ID
     * @param newName 새로운 이름
     * @return 성공 여부
     */
    suspend fun changeFriendName(friendId: String, newName: UserName): CustomResult<Unit, Exception>

    /**
     * 친구 프로필 이미지 변경
     * @param friendId 친구 ID
     * @param newProfileImageUrl 새로운 프로필 이미지 URL
     * @return 성공 여부
     */
    suspend fun changeFriendProfileImage(
        friendId: String,
        newProfileImageUrl: ImageUrl?
    ): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * 친구 존재 여부 확인
     * @param friendId 친구 ID
     * @return 존재 여부
     */
    suspend fun friendExists(friendId: String): Boolean

    /**
     * 친구 이름 중복 확인
     * @param name 친구 이름
     * @param excludeFriendId 제외할 친구 ID (수정시 자기 자신 제외)
     * @return 중복 여부
     */
    suspend fun nameExists(name: UserName, excludeFriendId: String? = null): Boolean

    /**
     * 상태별 친구 수 조회
     * @param status 친구 상태
     * @return 해당 상태의 친구 수
     */
    suspend fun getFriendCountByStatus(status: FriendStatus): Int

    /**
     * 전체 친구 수 조회
     * @return 친구 수
     */
    suspend fun getTotalFriendCount(): Int

    /**
     * 수락된 친구 수 조회
     * @return 수락된 친구 수
     */
    suspend fun getAcceptedFriendCount(): Int

    /**
     * 보낸 친구 요청 수 조회
     * @return 보낸 친구 요청 수
     */
    suspend fun getSentFriendRequestCount(): Int

    /**
     * 받은 친구 요청 수 조회
     * @return 받은 친구 요청 수
     */
    suspend fun getReceivedFriendRequestCount(): Int

    /**
     * 차단된 친구 수 조회
     * @return 차단된 친구 수
     */
    suspend fun getBlockedFriendCount(): Int

    /**
     * 모든 친구 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllFriends(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 친구 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 친구 목록
     */
    suspend fun getFriendsUpdatedAfter(timestamp: Instant): List<Friend>

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * @param friendId 친구 ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        friendId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>
}