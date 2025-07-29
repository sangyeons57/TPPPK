package com.example.domain.repository.infrastructure

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.sync.OutboxStatus

/**
 * Outbox Repository Interface for production-level sync infrastructure
 * 공용 인프라 Repository로 모든 도메인에서 사용
 *
 * 🎯 책임:
 * - Outbox 패턴 구현 (enqueue/lease/ack/cleanup)
 * - 순서 보장 및 중복 방지
 * - 재시도 로직 및 백오프 전략
 * - 배치 처리 최적화
 *
 * 📋 아키텍처:
 * - Repository → DataSource → DAO 패턴 준수
 * - 도메인 Repository들이 이 인프라 Repository를 주입받아 사용
 * - SyncManager에서 lease/ack 패턴으로 처리
 */
interface OutboxRepository {

    // === 기본 Outbox 작업 ===

    /**
     * Outbox에 새 작업 추가 (enqueue)
     *
     * @param collectionName Firestore 컬렉션 이름
     * @param documentId 엔티티 ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload JSON 직렬화된 데이터 (선택적)
     * @param orderingKey 순서 보장을 위한 키 (기본값: collectionName_documentId)
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun enqueue(
        collectionName: String,
        documentId: String,
        operation: String,
        payload: String? = null,
        orderingKey: String? = null
    ): CustomResult<Unit, Exception>

    /**
     * 여러 Outbox 작업을 배치로 추가
     * 트랜잭션으로 처리하여 일관성 보장
     */
    suspend fun enqueueBatch(
        operations: List<OutboxOperation>
    ): CustomResult<Unit, Exception>

    /**
     * 처리 가능한 작업들을 임대 (lease)
     * SyncManager에서 사용하는 핵심 메서드
     *
     * @param batchSize 한 번에 가져올 작업 수
     * @param leaseTimeoutMs 임대 만료 시간 (밀리초)
     * @return 임대된 작업 목록
     */
    suspend fun leasePendingOperations(
        batchSize: Int = 10,
        leaseTimeoutMs: Long = 300000L // 5분
    ): CustomResult<List<OutboxOperation>, Exception>

    /**
     * 특정 orderingKey의 다음 작업 임대
     * 순서 보장이 필요한 작업들 처리용
     */
    suspend fun leaseNextByOrderingKey(
        orderingKey: String,
        leaseTimeoutMs: Long = 300000L
    ): CustomResult<OutboxOperation?, Exception>

    /**
     * 작업 처리 완료 확인 (ack)
     *
     * @param operationId 완료된 작업 ID
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun acknowledgeCompletion(
        operationId: String
    ): CustomResult<Unit, Exception>

    /**
     * 작업 처리 실패 처리 (nack)
     * 재시도 카운터 증가 및 다음 시도 시간 설정
     *
     * @param operationId 실패한 작업 ID
     * @param errorMessage 실패 원인
     * @param scheduleRetry 재시도 스케줄링 여부
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun acknowledgeFailure(
        operationId: String,
        errorMessage: String,
        scheduleRetry: Boolean = true
    ): CustomResult<Unit, Exception>

    // === 조회 및 모니터링 ===

    /**
     * 특정 컬렉션의 대기 중인 작업 수 조회
     */
    suspend fun getPendingCount(collectionName: String): CustomResult<Int, Exception>

    /**
     * 전체 대기 중인 작업 수 조회
     */
    suspend fun getTotalPendingCount(): CustomResult<Int, Exception>

    /**
     * 전체 대기 중인 작업 수 조회 (SyncManager 호환)
     */
    suspend fun getPendingOperationCount(): CustomResult<Int, Exception>

    /**
     * 실패한 작업들 조회 (재시도 한계 도달)
     */
    suspend fun getFailedOperations(
        limit: Int = 100
    ): CustomResult<List<OutboxOperation>, Exception>

    /**
     * 특정 엔티티의 대기 중인 작업들 조회
     */
    suspend fun getPendingOperationsForEntity(
        collectionName: String,
        documentId: String
    ): CustomResult<List<OutboxOperation>, Exception>

    /**
     * 가장 오래된 처리되지 않은 작업 조회
     */
    suspend fun getOldestPendingOperation(): CustomResult<OutboxOperation?, Exception>

    // === 정리 및 유지보수 ===

    /**
     * 완료된 오래된 작업들 정리
     *
     * @param olderThanMs 이 시간보다 오래된 작업들 정리 (밀리초)
     * @return 정리된 작업 수
     */
    suspend fun cleanupCompletedOperations(
        olderThanMs: Long = 86400000L // 24시간
    ): CustomResult<Int, Exception>

    /**
     * 임대 시간이 만료된 작업들을 PENDING 상태로 되돌리기
     * SyncManager 장애시 stuck 된 작업들 복구용
     */
    suspend fun resetExpiredLeases(): CustomResult<Int, Exception>

    /**
     * 최대 재시도 횟수를 초과한 작업들을 FAILED로 표시
     */
    suspend fun markExceededRetriesAsFailed(): CustomResult<Int, Exception>

    /**
     * 모든 작업 제거 (개발/테스트용)
     */
    suspend fun clearAll(): CustomResult<Unit, Exception>

    // === SyncManager 지원 메서드 ===

    /**
     * 실패한 작업들 재시도
     *
     * @param maxRetries 최대 재시도 횟수
     * @return 재시도된 작업 수
     */
    suspend fun retryFailedOperations(maxRetries: Int): CustomResult<Int, Exception>

    /**
     * 재시도 카운트 증가
     *
     * @param operationId 작업 ID
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun incrementRetryCount(operationId: String): CustomResult<Unit, Exception>

    /**
     * 작업을 실패 상태로 마킹
     *
     * @param operationId 작업 ID
     * @param errorMessage 실패 원인
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun markAsFailed(
        operationId: String,
        errorMessage: String
    ): CustomResult<Unit, Exception>

    // === 통계 및 헬스체크 ===

    /**
     * Outbox 상태 통계 조회
     */
    suspend fun getStatistics(): CustomResult<OutboxStatistics, Exception>

    /**
     * 특정 orderingKey의 처리 상태 확인
     */
    suspend fun getOrderingKeyStatus(orderingKey: String): CustomResult<OrderingKeyStatus, Exception>
}

/**
 * Outbox 작업을 나타내는 데이터 클래스
 */
data class OutboxOperation(
    val id: String,
    val idempotencyKey: String,
    val orderingKey: String,
    val collectionName: String,
    val documentId: String,
    val operation: String,
    val status: OutboxStatus,
    val payload: String? = null,
    val localTimestamp: Long,
    val scheduledAt: Long? = null,
    val retries: Int = 0,
    val maxRetries: Int = 3,
    val errorMessage: String? = null,
    val lastProcessedAt: Long? = null
)

/**
 * Outbox 통계 정보
 */
data class OutboxStatistics(
    val totalCount: Int,
    val pendingCount: Int,
    val processingCount: Int,
    val completedCount: Int,
    val failedCount: Int,
    val oldestPendingTimestamp: Long?,
    val averageProcessingTimeMs: Long?
)

/**
 * OrderingKey 처리 상태
 */
data class OrderingKeyStatus(
    val orderingKey: String,
    val pendingCount: Int,
    val processingCount: Int,
    val isBlocked: Boolean,
    val nextScheduledAt: Long?
)