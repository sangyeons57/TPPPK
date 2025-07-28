package com.example.domain.repository.local.base

import com.example.core_common.result.CustomResult
import kotlinx.coroutines.flow.Flow

/**
 * Base Local Repository Interface (SSOT Pattern)
 * 모든 Local Repository의 공통 CRUD 기능 정의
 *
 * 🎯 역할:
 * - 기본 CRUD 작업 표준화
 * - Flow 기반 실시간 UI 업데이트 지원
 * - 일관된 API 제공으로 개발 효율성 향상
 * - Room Database 전용 (네트워크 호출 금지)
 *
 * 📋 공통 CRUD 메서드:
 * - observe: Flow 기반 실시간 관찰
 * - get: 단순 읽기 작업
 * - save: 생성/수정 작업 (Outbox 포함)
 * - delete: 삭제 작업 (Soft Delete + Outbox)
 *
 * @param T 엔티티 타입 (Category, Project, User 등)
 */
interface BaseLocalRepository<T> : SyncableRepository<T> {

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 엔티티를 실시간 관찰
     * UI에서 데이터 변경사항을 즉시 반영
     *
     * @param entityId 엔티티 ID
     * @return 엔티티 Flow (null 가능)
     */
    fun observeEntityById(entityId: String): Flow<T?>

    /**
     * 모든 엔티티를 실시간 관찰
     * 목록 UI에서 데이터 변경사항을 즉시 반영
     *
     * @return 엔티티 목록 Flow
     */
    fun observeAllEntities(): Flow<List<T>>

    /**
     * 특정 엔티티의 updatedAt 필드 변경을 실시간 관찰
     * 동기화 상태 모니터링용
     *
     * @param entityId 엔티티 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeEntityUpdatedAt(entityId: String): Flow<Long?>

    // === 단순 읽기 작업 ===

    /**
     * 엔티티 ID로 조회
     *
     * @param entityId 엔티티 ID
     * @return 엔티티 (없으면 null)
     */
    suspend fun getEntityById(entityId: String): CustomResult<T?, Exception>

    /**
     * 여러 엔티티 ID로 조회
     *
     * @param entityIds 엔티티 ID 목록
     * @return 엔티티 목록
     */
    suspend fun getEntitiesByIds(entityIds: List<String>): CustomResult<List<T>, Exception>

    /**
     * 전체 엔티티 조회
     *
     * @param limit 제한 개수 (null이면 전체)
     * @return 엔티티 목록
     */
    suspend fun getAllEntities(limit: Int? = null): CustomResult<List<T>, Exception>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 엔티티 저장 (생성/수정)
     * Outbox에 동기화 작업 자동 추가
     *
     * @param entity 저장할 엔티티
     * @return 성공 여부
     */
    suspend fun saveEntity(entity: T): CustomResult<Unit, Exception>

    /**
     * 엔티티 대량 저장 (동기화용)
     * 서버에서 받은 데이터 일괄 저장 (Outbox 추가 안 함)
     *
     * @param entities 저장할 엔티티 목록
     * @return 성공 여부
     */
    suspend fun saveEntities(entities: List<T>): CustomResult<Unit, Exception>

    /**
     * 엔티티 삭제 (Soft Delete)
     * Outbox에 삭제 작업 자동 추가
     *
     * @param entityId 엔티티 ID
     * @return 성공 여부
     */
    suspend fun deleteEntity(entityId: String): CustomResult<Unit, Exception>

    // === SyncableRepository 구체적 구현 ===

    /**
     * findUpdatedAfter 편의 메서드 추가
     * Sync UseCase에서 사용하는 메서드명과 일치
     */
    suspend fun findUpdatedAfter(timestamp: Instant): CustomResult<List<T>, Exception> = 
        getEntitiesUpdatedAfter(timestamp)

    /**
     * deleteAllEntities 편의 메서드 추가
     * Sync UseCase에서 사용하는 메서드명과 일치
     */
    suspend fun deleteAllEntities(): CustomResult<Unit, Exception> = clearAllEntities()

    /**
     * clearAllEntities의 구체적 구현
     * (SyncableRepository에서 상속)
     */
    override suspend fun clearAllEntities(): CustomResult<Unit, Exception>

    /**
     * getTotalEntityCount의 구체적 구현
     * (SyncableRepository에서 상속)
     */
    override suspend fun getTotalEntityCount(): CustomResult<Int, Exception>

    /**
     * entityExists의 구체적 구현
     * (SyncableRepository에서 상속)
     */
    override suspend fun entityExists(entityId: String): CustomResult<Boolean, Exception>
}