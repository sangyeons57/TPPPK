package com.example.domain.repository.local.base

import com.example.core_common.result.CustomResult
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
 *
 * 📋 공통 동기화 메서드:
 * - getXXXUpdatedAfter: 증분 동기화용 데이터 조회
 * - addToOutbox: 로컬 변경사항을 동기화 대기열에 추가
 * - clearAll: 초기화 (테스트/재동기화용)
 * - getCount: 통계/검증용
 *
 * @param T 엔티티 타입 (Category, Project, User 등)
 */
interface SyncableRepository<T> {

    // === 증분 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 엔티티 조회
     * 서버로부터 증분 동기화시 사용
     *
     * @param timestamp 기준 시간 (UTC)
     * @return 업데이트된 엔티티 목록
     */
    suspend fun getEntitiesUpdatedAfter(timestamp: Instant): CustomResult<List<T>, Exception>

    // === Outbox 패턴 지원 ===

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * 로컬 변경사항을 서버에 동기화하기 위한 대기열
     *
     * @param entityId 엔티티 ID
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
     * 모든 엔티티 삭제 (초기화)
     * 테스트나 전체 재동기화시 사용
     *
     * @return 성공 여부
     */
    suspend fun clearAllEntities(): CustomResult<Unit, Exception>

    /**
     * 전체 엔티티 수 조회
     * 통계 및 동기화 검증용
     *
     * @return 엔티티 수
     */
    suspend fun getTotalEntityCount(): CustomResult<Int, Exception>

    // === 존재 여부 확인 ===

    /**
     * 엔티티 존재 여부 확인
     * 중복 방지 및 검증용
     *
     * @param entityId 엔티티 ID
     * @return 존재 여부
     */
    suspend fun entityExists(entityId: String): CustomResult<Boolean, Exception>
}