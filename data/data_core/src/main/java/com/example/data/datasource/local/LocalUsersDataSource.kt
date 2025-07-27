package com.example.data.datasource.local

import com.example.domain.model.base.User
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 로컬 사용자 데이터 저장소 인터페이스
 * 3-tier 클라이언트 주도 동기화 아키텍처를 지원합니다
 */
interface LocalUsersDataSource {

    // === 기본 CRUD 작업 ===

    /**
     * 사용자 ID로 단일 사용자 조회
     * @param userId 사용자 ID
     * @return 사용자 정보 (없으면 null)
     */
    suspend fun getUserById(userId: String): User?

    /**
     * 모든 사용자 목록 조회
     * @return 사용자 목록
     */
    suspend fun getAllUsers(): List<User>

    /**
     * 이메일로 사용자 조회
     * @param email 사용자 이메일
     * @return 사용자 정보 (없으면 null)
     */
    suspend fun getUserByEmail(email: String): User?

    /**
     * 계정 상태별 사용자 목록 조회
     * @param accountStatus 계정 상태 (ACTIVE, SUSPENDED, WITHDRAWN)
     * @return 사용자 목록
     */
    suspend fun getUsersByAccountStatus(accountStatus: String): List<User>

    /**
     * 단일 사용자 정보 저장
     * @param user 저장할 사용자 정보
     */
    suspend fun saveUser(user: User)

    /**
     * 사용자 목록 배치 저장
     * @param users 저장할 사용자 목록
     */
    suspend fun saveUsers(users: List<User>)

    /**
     * 사용자 삭제
     * @param userId 삭제할 사용자 ID
     */
    suspend fun deleteUser(userId: String)

    // === 3-tier 동기화 지원 ===

    /**
     * 특정 시점 이후 업데이트된 사용자들을 조회 (증분 동기화용)
     * @param timestamp 기준 시간
     * @return 업데이트된 사용자 목록
     */
    suspend fun getUsersUpdatedAfter(timestamp: Instant): List<User>

    /**
     * 사용자 실시간 관찰
     * @param userId 사용자 ID
     * @return 사용자 정보 Flow
     */
    fun observeUserById(userId: String): Flow<User?>

    /**
     * 모든 사용자 실시간 관찰
     * @return 사용자 목록 Flow
     */
    fun observeAllUsers(): Flow<List<User>>

    // === Outbox 관리 ===

    /**
     * 사용자 변경사항을 Outbox에 기록
     * @param userId 사용자 ID
     * @param operation 작업 유형 (CREATE, UPDATE, DELETE)
     * @param payload 변경 데이터 (선택적)
     */
    suspend fun addToOutbox(userId: String, operation: String, payload: String? = null)

    /**
     * 대기 중인 Outbox 작업 목록 조회
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingOutboxOperations(): List<UserOutboxOperation>

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
     * 사용자 존재 여부 확인
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    suspend fun userExists(userId: String): Boolean

    /**
     * 전체 사용자 수 조회
     * @return 사용자 수
     */
    suspend fun getUserCount(): Int

    /**
     * 모든 사용자 데이터 삭제 (개발/테스트용)
     */
    suspend fun clearAllUsers()
}

/**
 * 사용자 Outbox 작업 정보
 */
data class UserOutboxOperation(
    val id: String,
    val userId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)