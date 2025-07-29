package com.example.domain.repository.local.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import java.time.Instant

/**
 * Syncable Repository Interface (SSOT Pattern)
 * 순수 동기화 전용 인터페이스 - Outbox 패턴과 증분 동기화 지원
 *
 * 🎯 역할:
 * - 서버 동기화를 위한 전용 인터페이스 제공
 * - Outbox 패턴 지원 (오프라인 우선 아키텍처)
 * - 증분 동기화 지원 (타임스탬프 기반)
 * - SyncMetadata 관리 (동기화 커서 및 상태)
 * - 동기화 관련 작업만 담당
 *
 * 📋 순수 동기화 메서드:
 * - 증분 동기화: getEntitiesUpdatedAfter
 * - Outbox 관리: addToOutbox, getPendingOutboxOperations 등
 * - SyncMetadata 관리: getLastSyncCursor, updateSyncCursor 등
 *
 * ❌ 일반 CRUD는 BaseLocalRepository에서 담당
 *
 * @param T 도메인 모델 타입 (Category, Project, User 등)
 */
interface SyncableRepository<T> where T : AggregateRoot {

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

    /**
     * 대기 중인 Outbox 작업 목록 조회
     * 동기화 처리를 위한 대기 작업 확인
     *
     * @return 대기 중인 Outbox 작업 목록
     */
    suspend fun getPendingOutboxOperations(): CustomResult<List<OutboxOperation>, Exception>

    /**
     * Outbox 작업 완료 처리
     * 서버 동기화 성공시 해당 작업을 Outbox에서 제거
     *
     * @param operationId Outbox 작업 ID
     * @return 성공 여부
     */
    suspend fun markOutboxOperationComplete(operationId: String): CustomResult<Unit, Exception>

    /**
     * 실패한 Outbox 작업 목록 조회
     * 재시도가 필요한 작업들 확인
     *
     * @return 실패한 Outbox 작업 목록
     */
    suspend fun getFailedOutboxOperations(): CustomResult<List<OutboxOperation>, Exception>

    // === SyncMetadata 관리 ===

    /**
     * 마지막 동기화 커서 조회
     * 증분 동기화의 시작점 결정
     *
     * @return 마지막 서버 커서 (타임스탬프)
     */
    suspend fun getLastSyncCursor(): CustomResult<Long?, Exception>

    /**
     * 동기화 커서 업데이트
     * 성공한 동기화 작업 후 커서 갱신
     *
     * @param cursor 새로운 서버 커서
     * @param timestamp 동기화 완료 시간
     * @return 성공 여부
     */
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long): CustomResult<Unit, Exception>

    /**
     * 마지막 성공 동기화 시간 조회
     * 동기화 건강 상태 모니터링용
     *
     * @return 마지막 성공 동기화 시간
     */
    suspend fun getLastSuccessfulSyncTime(): CustomResult<Long?, Exception>
}

/**
 * Outbox 작업 정보
 * SyncableRepository에서 사용하는 Outbox 작업 데이터
 */
data class OutboxOperation(
    val id: String,
    val entityId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)