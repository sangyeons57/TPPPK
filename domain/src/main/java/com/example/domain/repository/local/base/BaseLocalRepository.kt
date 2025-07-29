package com.example.domain.repository.local.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
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
 * - get/save/delete: 기본 CRUD 작업
 * - 통계 및 유틸리티 메서드
 *
 * ✅ SyncableRepository와 독립적으로 운영
 * - 동기화 기능은 SyncableRepository에서 별도 관리
 * - 순수 CRUD 기능만 담당
 *
 * @param T 도메인 모델 타입 (Category, Project, User 등)
 */
interface BaseLocalRepository<T> where T : AggregateRoot {

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 도메인 모델을 실시간 관찰
     * UI에서 데이터 변경사항을 즉시 반영
     *
     * @param entityId 도메인 모델 ID
     * @return 도메인 모델 Flow (null 가능)
     */
    fun observeEntityById(entityId: String): Flow<T?>

    /**
     * 모든 도메인 모델을 실시간 관찰
     * 목록 UI에서 데이터 변경사항을 즉시 반영
     *
     * @return 도메인 모델 목록 Flow
     */
    fun observeAllEntities(): Flow<List<T>>

    /**
     * 특정 도메인 모델의 updatedAt 필드 변경을 실시간 관찰
     * 동기화 상태 모니터링용
     *
     * @param entityId 도메인 모델 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeEntityUpdatedAt(entityId: String): Flow<Long?>

    // === 단순 읽기 작업 ===

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

    // === 기본 CRUD 작업 ===

    /**
     * 도메인 모델 저장/업데이트
     * UI 및 UseCase에서 사용하는 기본 저장 기능
     *
     * @param entity 저장할 도메인 모델
     * @return 성공 여부
     */
    suspend fun saveEntity(entity: T): CustomResult<Unit, Exception>

    /**
     * ID로 도메인 모델 조회
     * 기본 조회 기능
     *
     * @param entityId 도메인 모델 ID
     * @return 조회된 도메인 모델 (nullable)
     */
    suspend fun getEntityById(entityId: String): CustomResult<T?, Exception>

    /**
     * 도메인 모델 삭제
     * 기본 삭제 기능
     *
     * @param entityId 삭제할 도메인 모델 ID
     * @return 성공 여부
     */
    suspend fun deleteEntity(entityId: String): CustomResult<Unit, Exception>

    /**
     * 엔티티 대량 저장 (동기화용)
     * 서버에서 받은 데이터 일괄 저장 (Outbox 추가 안 함)
     *
     * @param entities 저장할 엔티티 목록
     * @return 성공 여부
     */
    suspend fun saveEntities(entities: List<T>): CustomResult<Unit, Exception>

    // === 통계 및 유틸리티 ===

    /**
     * 모든 도메인 모델 삭제 (초기화)
     * 테스트나 전체 재동기화시 사용
     *
     * @return 성공 여부
     */
    suspend fun clearAllEntities(): CustomResult<Unit, Exception>

    /**
     * deleteAllEntities 편의 메서드 (clearAllEntities와 동일)
     * Sync UseCase에서 사용하는 메서드명과 일치
     */
    suspend fun deleteAllEntities(): CustomResult<Unit, Exception> = clearAllEntities()

    /**
     * 전체 도메인 모델 수 조회
     * 통계 및 검증용
     *
     * @return 도메인 모델 수
     */
    suspend fun getTotalEntityCount(): CustomResult<Int, Exception>

    /**
     * 도메인 모델 존재 여부 확인
     * 중복 방지 및 검증용
     *
     * @param entityId 도메인 모델 ID
     * @return 존재 여부
     */
    suspend fun entityExists(entityId: String): CustomResult<Boolean, Exception>
}