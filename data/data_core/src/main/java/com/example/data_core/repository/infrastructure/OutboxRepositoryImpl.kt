package com.example.data_core.repository.infrastructure

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.SyncOutboxDataSource
import com.example.data_model.local.OutboxEntity
import com.example.domain.model.vo.sync.OutboxStatus
import com.example.domain.repository.infrastructure.OrderingKeyStatus
import com.example.domain.repository.infrastructure.OutboxOperation
import com.example.domain.repository.infrastructure.OutboxRepository
import com.example.domain.repository.infrastructure.OutboxStatistics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OutboxRepository 구현체
 * SyncOutboxDataSource를 통해 Outbox 패턴의 핵심 기능 구현
 *
 * 🚀 Production Features:
 * - Lease/Ack 패턴으로 동시성 제어
 * - Idempotency 키로 중복 방지
 * - Ordering 키로 순서 보장
 * - 지수 백오프 재시도 전략
 * - 배치 처리 최적화
 *
 * 📋 아키텍처:
 * - Repository → DataSource → DAO 패턴 준수
 * - 모든 DAO 호출을 DataSource로 위임
 * - 비즈니스 로직과 데이터 접근 로직 분리
 */
@Singleton
class OutboxRepositoryImpl @Inject constructor(
    private val syncOutboxDataSource: SyncOutboxDataSource
) : OutboxRepository {

    companion object {
        private const val TAG = "OutboxRepository"
        private const val DEFAULT_MAX_RETRIES = 3
        private const val DEFAULT_LEASE_TIMEOUT_MS = 300000L // 5분
        private const val DEFAULT_CLEANUP_AGE_MS = 86400000L // 24시간
    }

    // === 기본 Outbox 작업 ===

    override suspend fun enqueue(
        collectionName: String,
        documentId: String,
        operation: String,
        payload: String?,
        orderingKey: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Enqueueing operation: $collectionName/$documentId/$operation")

            val actualOrderingKey =
                orderingKey ?: OutboxEntity.generateOrderingKey(collectionName, documentId)
            val idempotencyKey = OutboxEntity.generateIdempotencyKey(
                collectionName, documentId, operation, payload?.hashCode()?.toString()
            )

            syncOutboxDataSource.addToOutbox(
                entityId = documentId,
                collectionName = collectionName,
                operation = operation,
                payload = payload,
                idempotencyKey = idempotencyKey,
                orderingKey = actualOrderingKey
            )

            Log.d(TAG, "Successfully enqueued operation: $collectionName/$documentId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue operation: $collectionName/$documentId", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun enqueueBatch(
        operations: List<OutboxOperation>
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Enqueueing batch of ${operations.size} operations")

            // 각 operation을 개별적으로 추가 (트랜잭션은 DataSource 레벨에서 처리)
            operations.forEach { op ->
                syncOutboxDataSource.addToOutbox(
                    entityId = op.documentId,
                    collectionName = op.collectionName,
                    operation = op.operation,
                    payload = op.payload,
                    idempotencyKey = op.idempotencyKey,
                    orderingKey = op.orderingKey
                )
            }

            Log.d(TAG, "Successfully enqueued batch of ${operations.size} operations")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue batch operations", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun leasePendingOperations(
        batchSize: Int,
        leaseTimeoutMs: Long
    ): CustomResult<List<OutboxOperation>, Exception> {
        return try {
            Log.d(TAG, "Leasing $batchSize pending operations")

            val outboxEntities =
                syncOutboxDataSource.leasePendingOperations(batchSize, leaseTimeoutMs)
            val operations = outboxEntities.map { entity -> entity.toOperation() }

            Log.d(TAG, "Successfully leased ${operations.size} operations")
            CustomResult.Success(operations)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to lease pending operations", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun leaseNextByOrderingKey(
        orderingKey: String,
        leaseTimeoutMs: Long
    ): CustomResult<OutboxOperation?, Exception> {
        return try {
            Log.d(TAG, "Leasing next operation for orderingKey: $orderingKey")

            val outboxEntity =
                syncOutboxDataSource.leaseNextByOrderingKey(orderingKey, leaseTimeoutMs)
            val operation = outboxEntity?.toOperation()

            Log.d(TAG, "Successfully leased operation for orderingKey: $orderingKey")
            CustomResult.Success(operation)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to lease operation for orderingKey: $orderingKey", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun acknowledgeCompletion(
        operationId: String
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Acknowledging completion for operation: $operationId")

            syncOutboxDataSource.markOperationComplete(operationId)

            Log.d(TAG, "Successfully acknowledged completion: $operationId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to acknowledge completion: $operationId", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun acknowledgeFailure(
        operationId: String,
        errorMessage: String,
        scheduleRetry: Boolean
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Acknowledging failure for operation: $operationId")

            syncOutboxDataSource.markOperationFailed(operationId, errorMessage, scheduleRetry)

            Log.d(TAG, "Successfully acknowledged failure: $operationId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to acknowledge failure: $operationId", e)
            CustomResult.Failure(e)
        }
    }

    // === 조회 및 모니터링 ===

    override suspend fun getPendingCount(collectionName: String): CustomResult<Int, Exception> {
        return try {
            val count = syncOutboxDataSource.getPendingCountByCollection(collectionName)
            CustomResult.Success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pending count for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun getTotalPendingCount(): CustomResult<Int, Exception> {
        return try {
            val count = syncOutboxDataSource.getTotalPendingCount()
            CustomResult.Success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get total pending count", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun getFailedOperations(limit: Int): CustomResult<List<OutboxOperation>, Exception> {
        return try {
            val outboxEntities = syncOutboxDataSource.getFailedOperations()
                .take(limit)
            val operations = outboxEntities.map { it.toOperation() }
            CustomResult.Success(operations)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get failed operations", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun getPendingOperationsForEntity(
        collectionName: String,
        documentId: String
    ): CustomResult<List<OutboxOperation>, Exception> {
        return try {
            val outboxEntities =
                syncOutboxDataSource.getPendingOperationsForEntity(collectionName, documentId)
            val operations = outboxEntities.map { it.toOperation() }
            CustomResult.Success(operations)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to get pending operations for entity: $collectionName/$documentId",
                e
            )
            CustomResult.Failure(e)
        }
    }

    override suspend fun getOldestPendingOperation(): CustomResult<OutboxOperation?, Exception> {
        return try {
            val outboxEntity = syncOutboxDataSource.getOldestPendingOperation()
            val operation = outboxEntity?.toOperation()
            CustomResult.Success(operation)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get oldest pending operation", e)
            CustomResult.Failure(e)
        }
    }

    // === 정리 및 유지보수 ===

    override suspend fun cleanupCompletedOperations(olderThanMs: Long): CustomResult<Int, Exception> {
        return try {
            Log.d(TAG, "Cleaning up completed operations older than ${olderThanMs}ms")

            val count = syncOutboxDataSource.cleanupCompletedOperations(olderThanMs)

            Log.d(TAG, "Successfully cleaned up $count completed operations")
            CustomResult.Success(count)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup completed operations", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun resetExpiredLeases(): CustomResult<Int, Exception> {
        return try {
            Log.d(TAG, "Resetting expired leases")

            val count = syncOutboxDataSource.resetExpiredLeases()

            Log.d(TAG, "Successfully reset $count expired leases")
            CustomResult.Success(count)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset expired leases", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun markExceededRetriesAsFailed(): CustomResult<Int, Exception> {
        return try {
            Log.d(TAG, "Marking exceeded retries as failed")

            val count = syncOutboxDataSource.markExceededRetriesAsFailed()

            Log.d(TAG, "Successfully marked $count operations as failed")
            CustomResult.Success(count)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark exceeded retries as failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun clearAll(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Clearing all outbox operations")

            syncOutboxDataSource.clearAllOperations()

            Log.d(TAG, "Successfully cleared all outbox operations")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all operations", e)
            CustomResult.Failure(e)
        }
    }

    // === SyncManager 지원 메서드 ===

    override suspend fun getPendingOperationCount(): CustomResult<Int, Exception> {
        return getTotalPendingCount()
    }

    override suspend fun retryFailedOperations(maxRetries: Int): CustomResult<Int, Exception> {
        return try {
            Log.d(TAG, "Retrying failed operations with maxRetries: $maxRetries")

            val retriedCount = syncOutboxDataSource.retryFailedOperations(maxRetries)

            Log.d(TAG, "Retried $retriedCount failed operations")
            CustomResult.Success(retriedCount)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to retry failed operations", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun incrementRetryCount(operationId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Incrementing retry count for operation: $operationId")

            syncOutboxDataSource.incrementRetryCount(operationId)

            Log.d(TAG, "Successfully incremented retry count for: $operationId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment retry count for operation: $operationId", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun markAsFailed(
        operationId: String,
        errorMessage: String
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Marking operation as failed: $operationId, error: $errorMessage")

            syncOutboxDataSource.markAsFailed(operationId, errorMessage)

            Log.d(TAG, "Successfully marked operation as failed: $operationId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark operation as failed: $operationId", e)
            CustomResult.Failure(e)
        }
    }

    // === 통계 및 헬스체크 ===

    override suspend fun getStatistics(): CustomResult<OutboxStatistics, Exception> {
        return try {
            val stats = syncOutboxDataSource.getStatistics()
            CustomResult.Success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get statistics", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun getOrderingKeyStatus(orderingKey: String): CustomResult<OrderingKeyStatus, Exception> {
        return try {
            val status = syncOutboxDataSource.getOrderingKeyStatus(orderingKey)
            CustomResult.Success(status)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ordering key status: $orderingKey", e)
            CustomResult.Failure(e)
        }
    }

    // === 헬퍼 메서드 ===

    /**
     * OutboxEntity를 OutboxOperation으로 변환
     */
    private fun OutboxEntity.toOperation(): OutboxOperation {
        return OutboxOperation(
            id = this.id,
            idempotencyKey = this.idempotencyKey,
            orderingKey = this.orderingKey,
            collectionName = this.collectionName,
            documentId = this.documentId,
            operation = this.operation,
            status = OutboxStatus.fromString(this.status),
            payload = this.payload,
            localTimestamp = this.localTimestamp,
            scheduledAt = this.scheduledAt,
            retries = this.retries,
            maxRetries = this.maxRetries,
            errorMessage = this.errorMessage,
            lastProcessedAt = this.lastProcessedAt
        )
    }
}