package com.example.domain.repository.local.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import java.time.Instant

/**
 * Syncable Repository Interface (SSOT Pattern)
 * 모든 Local Repository의 공통 동기화 기능 정의
 *
 * 🎯 역할:
 * - 서버 동기화를 위한 공통 인터페이스 제공
 * - Outbox 패턴 지원 (오프라인 우선 아키텍처)
 * - 증분 동기화 지원 (타임스탬프 기반)
 * - 일관된 동기화 API 보장
 * - 제네릭 Sync UseCase 지원
 *
 * 📋 공통 동기화 메서드:
 * - getXXXUpdatedAfter: 증분 동기화용 데이터 조회
 * - addToOutbox: 로컬 변경사항을 동기화 대기열에 추가
 * - clearAll: 초기화 (테스트/재동기화용)
 * - getCount: 통계/검증용
 * - saveEntity/getEntityById: 제네릭 UseCase용 CRUD
 *
 * @param T 도메인 모델 타입 (Category, Project, User 등)
 */
interface SyncableRepository<T> where T : AggregateRoot {

    // === 컬렉션 이름 정의 ===

    /**
     * Firestore 컬렉션 이름 반환
     * 제네릭 UseCase에서 동적으로 컬렉션 설정용
     */
    val collectionName: String

    // === 기본 CRUD (제네릭 UseCase용) ===

    /**
     * 도메인 모델 저장/업데이트
     * 제네릭 UseCase에서 사용
     *
     * @param entity 저장할 도메인 모델
     * @return 성공 여부
     */
    suspend fun saveEntity(entity: T): CustomResult<Unit, Exception>

    /**
     * ID로 도메인 모델 조회
     * 제네릭 UseCase에서 사용
     *
     * @param entityId 도메인 모델 ID
     * @return 조회된 도메인 모델 (nullable)
     */
    suspend fun getEntityById(entityId: String): CustomResult<T?, Exception>

    /**
     * 도메인 모델 삭제
     * 제네릭 UseCase에서 사용
     *
     * @param entityId 삭제할 도메인 모델 ID
     * @return 성공 여부
     */
    suspend fun deleteEntity(entityId: String): CustomResult<Unit, Exception>

    // === 증분 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 도메인 모델 조회
     * 서버로부터 증분 동기화시 사용
     *
     * @param timestamp 기준 시간 (UTC)
     * @return 업데이트된 도메인 모델 목록
     */
    suspend fun getEntitiesUpdatedAfter(timestamp: Instant): CustomResult<List<T>, Exception>

    // === Outbox 패턴 지원 ===

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * 로컬 변경사항을 서버에 동기화하기 위한 대기열
     *
     * @param entityId 도메인 모델 ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON 직렬화된 변경사항)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        entityId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>

    // === 초기화 및 통계 ===

    /**
     * 모든 도메인 모델 삭제 (초기화)
     * 테스트나 전체 재동기화시 사용
     *
     * @return 성공 여부
     */
    suspend fun clearAllEntities(): CustomResult<Unit, Exception>

    /**
     * 전체 도메인 모델 수 조회
     * 통계 및 동기화 검증용
     *
     * @return 도메인 모델 수
     */
    suspend fun getTotalEntityCount(): CustomResult<Int, Exception>

    // === 존재 여부 확인 ===

    /**
     * 도메인 모델 존재 여부 확인
     * 중복 방지 및 검증용
     *
     * @param entityId 도메인 모델 ID
     * @return 존재 여부
     */
    suspend fun entityExists(entityId: String): CustomResult<Boolean, Exception>
}