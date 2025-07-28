package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local User Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (RemoteUserRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - User 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - 로컬 검색 및 필터링
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeUserById
 * - observeAllEntities -> observeAllUsers
 * - observeEntityUpdatedAt -> observeUserUpdatedAt
 * - getEntityById -> getUserById
 * - getEntitiesByIds -> getUsersByIds
 * - getAllEntities -> getAllUsers
 * - saveEntity -> saveUser
 * - saveEntities -> saveUsers
 * - deleteEntity -> deleteUser
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalUserRepository : BaseLocalRepository<User> {

    // === BaseLocalRepository 메서드 (구현체에서 사용자 전용 메서드로 매핑) ===
    // observeEntityById -> observeUserById
    // observeAllEntities -> observeAllUsers  
    // observeEntityUpdatedAt -> observeUserUpdatedAt
    // getEntityById -> getUserById
    // getEntitiesByIds -> getUsersByIds
    // getAllEntities -> getAllUsers
    // saveEntity -> saveUser
    // saveEntities -> saveUsers
    // deleteEntity -> deleteUser
    // getEntitiesUpdatedAfter -> getUsersUpdatedAfter
    // clearAllEntities -> clearAllUsers
    // getTotalEntityCount -> getTotalUserCount
    // entityExists -> userExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 사용자를 실시간 관찰
     * @param userId 사용자 ID
     * @return 사용자 Flow (null 가능)
     */
    fun observeUserById(userId: String): Flow<User?>

    /**
     * 주어진 이름(닉네임)과 정확히 일치하는 사용자를 실시간 관찰
     * @param name 사용자 이름
     * @return 사용자 Flow
     */
    fun observeByName(name: UserName): Flow<User?>

    /**
     * 주어진 이름(닉네임)을 포함하는 사용자 목록을 실시간 관찰
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 사용자 목록 Flow
     */
    fun observeAllByName(name: String, limit: Int = 10): Flow<List<User>>

    /**
     * 주어진 이메일과 정확히 일치하는 사용자를 실시간 관찰
     * @param email 이메일 주소
     * @return 사용자 Flow
     */
    fun observeByEmail(email: String): Flow<User?>

    /**
     * 주어진 ID 목록에 해당하는 사용자 목록을 실시간 관찰
     * @param userIds 사용자 ID 목록
     * @return 사용자 목록 Flow
     */
    fun observeUsers(userIds: List<String>): Flow<List<User>>

    /**
     * 특정 사용자의 updatedAt 필드 변경을 실시간 관찰
     * @param userId 사용자 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeUserUpdatedAt(userId: String): Flow<Long?>

    /**
     * 모든 사용자를 실시간 관찰
     * @return 전체 사용자 목록 Flow
     */
    fun observeAllUsers(): Flow<List<User>>

    // === 단순 읽기 작업 ===

    /**
     * 사용자 ID로 조회
     * @param userId 사용자 ID
     * @return 사용자 (없으면 null)
     */
    suspend fun getUserById(userId: String): User?

    /**
     * 이메일로 사용자 조회
     * @param email 이메일 주소
     * @return 사용자 (없으면 null)
     */
    suspend fun getUserByEmail(email: String): User?

    /**
     * 사용자 이름으로 조회 (정확히 일치)
     * @param name 사용자 이름
     * @return 사용자 (없으면 null)
     */
    suspend fun getUserByName(name: UserName): User?

    /**
     * 사용자 이름으로 검색 (부분 일치)
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 사용자 목록
     */
    suspend fun searchUsersByName(name: String, limit: Int = 10): List<User>

    /**
     * 여러 사용자 ID로 조회
     * @param userIds 사용자 ID 목록
     * @return 사용자 목록
     */
    suspend fun getUsersByIds(userIds: List<String>): List<User>

    /**
     * 전체 사용자 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 사용자 목록
     */
    suspend fun getAllUsers(limit: Int? = null): List<User>

    /**
     * 특정 상태의 사용자들 조회
     * @param accountStatus 계정 상태
     * @return 사용자 목록
     */
    suspend fun getUsersByStatus(accountStatus: String): List<User>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 사용자 저장 (생성/수정)
     * @param user 저장할 사용자
     * @return 성공 여부
     */
    suspend fun saveUser(user: User): CustomResult<Unit, Exception>

    /**
     * 사용자 대량 저장 (동기화용)
     * @param users 저장할 사용자 목록
     * @return 성공 여부
     */
    suspend fun saveUsers(users: List<User>): CustomResult<Unit, Exception>

    /**
     * 사용자 삭제 (Soft Delete)
     * @param userId 사용자 ID
     * @return 성공 여부
     */
    suspend fun deleteUser(userId: String): CustomResult<Unit, Exception>

    /**
     * 사용자 프로필 업데이트 (로컬)
     * @param userId 사용자 ID
     * @param name 새로운 이름 (nullable)
     * @param memo 새로운 메모 (nullable)
     * @param profileImageUrl 새로운 프로필 이미지 URL (nullable)
     * @return 성공 여부
     */
    suspend fun updateUserProfile(
        userId: String,
        name: String? = null,
        memo: String? = null,
        profileImageUrl: String? = null
    ): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * 사용자 존재 여부 확인
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    suspend fun userExists(userId: String): Boolean

    /**
     * 이메일 중복 확인
     * @param email 이메일 주소
     * @return 중복 여부
     */
    suspend fun emailExists(email: String): Boolean

    /**
     * 사용자 이름 중복 확인
     * @param name 사용자 이름
     * @return 중복 여부
     */
    suspend fun nameExists(name: UserName): Boolean

    /**
     * 전체 사용자 수 조회
     * @return 사용자 수
     */
    suspend fun getTotalUserCount(): Int

    /**
     * 활성 사용자 수 조회
     * @return 활성 사용자 수
     */
    suspend fun getActiveUserCount(): Int

    /**
     * 모든 사용자 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllUsers(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 사용자 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 사용자 목록
     */
    suspend fun getUsersUpdatedAfter(timestamp: Instant): List<User>

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * @param userId 사용자 ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        userId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>
}