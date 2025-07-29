package com.example.data_core.datasource.local

import android.util.Log
import com.example.data_core.dao.SyncOutboxDao
import com.example.data_model.local.OutboxEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync Outbox Data Source Implementation
 * SyncOutboxDao를 통한 동기화 Outbox 작업 관리
 *
 * 🎯 역할:
 * - SyncOutboxDao 래핑 및 추상화
 * - 비즈니스 로직 처리 (UUID 생성, 타임스탬프 설정)
 * - 에러 처리 및 로깅
 * - Repository 계층과 DAO 계층 분리
 *
 * 📋 구현 특징:
 * - 모든 DAO 호출을 래핑하여 일관된 인터페이스 제공
 * - OutboxEntity 생성시 필요한 메타데이터 자동 설정
 * - 예외 처리 및 로깅으로 디버깅 지원
 */
@Singleton
class SyncOutboxDataSourceImpl @Inject constructor(
    private val syncOutboxDao: SyncOutboxDao
) : SyncOutboxDataSource {

    companion object {
        private const val TAG = "SyncOutboxDataSource"
    }

    // === Basic Operations ===

    override suspend fun addToOutbox(
        entityId: String,
        collectionName: String,
        operation: String,
        payload: String?
    ) {
        try {
            Log.d(
                TAG,
                "Adding to outbox: collection=$collectionName, entity=$entityId, operation=$operation"
            )

            val outboxEntity = OutboxEntity(
                id = UUID.randomUUID().toString(),
                collectionName = collectionName,
                documentId = entityId,
                operation = operation,
                payload = payload,
                localTimestamp = System.currentTimeMillis(),
                retries = 0
            )

            syncOutboxDao.insertOperation(outboxEntity)
            Log.d(TAG, "Successfully added to outbox: ${outboxEntity.id}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add to outbox: collection=$collectionName, entity=$entityId", e)
            throw e
        }
    }

    override suspend fun addMultipleToOutbox(operations: List<OutboxEntity>) {
        try {
            Log.d(TAG, "Adding ${operations.size} operations to outbox")
            syncOutboxDao.insertOperations(operations)
            Log.d(TAG, "Successfully added ${operations.size} operations to outbox")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add ${operations.size} operations to outbox", e)
            throw e
        }
    }

    // === Query Operations ===

    override suspend fun getPendingOperationsByCollection(collectionName: String): List<OutboxEntity> {
        return try {
            Log.d(TAG, "Getting pending operations for collection: $collectionName")
            val operations = syncOutboxDao.getPendingOperationsByCollection(collectionName)
            Log.d(
                TAG,
                "Found ${operations.size} pending operations for collection: $collectionName"
            )
            operations

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pending operations for collection: $collectionName", e)
            throw e
        }
    }

    override suspend fun getAllPendingOperations(): List<OutboxEntity> {
        return try {
            Log.d(TAG, "Getting all pending operations")
            val operations = syncOutboxDao.getAllPendingOperations()
            Log.d(TAG, "Found ${operations.size} total pending operations")
            operations

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all pending operations", e)
            throw e
        }
    }

    override suspend fun getOperationsByEntity(
        collectionName: String,
        entityId: String
    ): List<OutboxEntity> {
        return try {
            Log.d(
                TAG,
                "Getting operations for entity: collection=$collectionName, entity=$entityId"
            )
            val operations = syncOutboxDao.getOperationsByEntity(collectionName, entityId)
            Log.d(TAG, "Found ${operations.size} operations for entity: $entityId")
            operations

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to get operations for entity: collection=$collectionName, entity=$entityId",
                e
            )
            throw e
        }
    }

    override suspend fun getFailedOperations(): List<OutboxEntity> {
        return try {
            Log.d(TAG, "Getting failed operations")
            val operations = syncOutboxDao.getFailedOperations()
            Log.d(TAG, "Found ${operations.size} failed operations")
            operations

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get failed operations", e)
            throw e
        }
    }

    // === Completion & Cleanup ===

    override suspend fun markOperationComplete(operationId: String) {
        try {
            Log.d(TAG, "Marking operation complete: $operationId")
            syncOutboxDao.deleteOperation(operationId)
            Log.d(TAG, "Successfully marked operation complete: $operationId")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark operation complete: $operationId", e)
            throw e
        }
    }

    override suspend fun removeOperationsByEntity(collectionName: String, entityId: String) {
        try {
            Log.d(
                TAG,
                "Removing operations for entity: collection=$collectionName, entity=$entityId"
            )
            syncOutboxDao.deleteOperationsByEntity(collectionName, entityId)
            Log.d(TAG, "Successfully removed operations for entity: $entityId")

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to remove operations for entity: collection=$collectionName, entity=$entityId",
                e
            )
            throw e
        }
    }

    override suspend fun removeOperationsByCollection(collectionName: String) {
        try {
            Log.d(TAG, "Removing operations for collection: $collectionName")
            syncOutboxDao.deleteOperationsByCollection(collectionName)
            Log.d(TAG, "Successfully removed operations for collection: $collectionName")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove operations for collection: $collectionName", e)
            throw e
        }
    }

    // === Retry Management ===

    override suspend fun incrementRetries(operationId: String) {
        try {
            Log.d(TAG, "Incrementing retries for operation: $operationId")
            val newTimestamp = System.currentTimeMillis()
            syncOutboxDao.incrementRetries(operationId, newTimestamp)
            Log.d(TAG, "Successfully incremented retries for operation: $operationId")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment retries for operation: $operationId", e)
            throw e
        }
    }

    override suspend fun resetRetries(operationId: String) {
        try {
            Log.d(TAG, "Resetting retries for operation: $operationId")
            syncOutboxDao.resetRetries(operationId)
            Log.d(TAG, "Successfully reset retries for operation: $operationId")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset retries for operation: $operationId", e)
            throw e
        }
    }

    override suspend fun getRetryableOperations(beforeTimestamp: Long): List<OutboxEntity> {
        return try {
            Log.d(TAG, "Getting retryable operations before timestamp: $beforeTimestamp")
            val operations = syncOutboxDao.getRetryableOperations(beforeTimestamp)
            Log.d(TAG, "Found ${operations.size} retryable operations")
            operations

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get retryable operations", e)
            throw e
        }
    }

    // === Real-time Observations ===

    override fun observeAllOperations(): Flow<List<OutboxEntity>> {
        Log.d(TAG, "Starting to observe all operations")
        return syncOutboxDao.observeAllOperations()
    }

    override fun observeOperationsByCollection(collectionName: String): Flow<List<OutboxEntity>> {
        Log.d(TAG, "Starting to observe operations for collection: $collectionName")
        return syncOutboxDao.observeOperationsByCollection(collectionName)
    }

    override fun observeFailedOperationCount(): Flow<Int> {
        Log.d(TAG, "Starting to observe failed operation count")
        return syncOutboxDao.observeFailedOperationCount()
    }

    // === Statistics ===

    override suspend fun getOperationCountByCollection(collectionName: String): Int {
        return try {
            Log.d(TAG, "Getting operation count for collection: $collectionName")
            val count = syncOutboxDao.getOperationCountByCollection(collectionName)
            Log.d(TAG, "Operation count for collection $collectionName: $count")
            count

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get operation count for collection: $collectionName", e)
            throw e
        }
    }

    override suspend fun getTotalOperationCount(): Int {
        return try {
            Log.d(TAG, "Getting total operation count")
            val count = syncOutboxDao.getTotalOperationCount()
            Log.d(TAG, "Total operation count: $count")
            count

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get total operation count", e)
            throw e
        }
    }

    override suspend fun hasEntityPendingOperations(
        collectionName: String,
        entityId: String
    ): Boolean {
        return try {
            Log.d(
                TAG,
                "Checking pending operations for entity: collection=$collectionName, entity=$entityId"
            )
            val hasPending = syncOutboxDao.hasEntityPendingOperations(collectionName, entityId)
            Log.d(TAG, "Entity $entityId has pending operations: $hasPending")
            hasPending

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to check pending operations for entity: collection=$collectionName, entity=$entityId",
                e
            )
            throw e
        }
    }

    override suspend fun getOldestOperationTimestamp(): Long? {
        return try {
            Log.d(TAG, "Getting oldest operation timestamp")
            val timestamp = syncOutboxDao.getOldestOperationTimestamp()
            Log.d(TAG, "Oldest operation timestamp: $timestamp")
            timestamp

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get oldest operation timestamp", e)
            throw e
        }
    }

    // === Maintenance ===

    override suspend fun deleteOldOperations(beforeTimestamp: Long) {
        try {
            Log.d(TAG, "Deleting operations older than timestamp: $beforeTimestamp")
            syncOutboxDao.deleteOldOperations(beforeTimestamp)
            Log.d(TAG, "Successfully deleted old operations")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete old operations", e)
            throw e
        }
    }

    override suspend fun deleteAllOperations() {
        try {
            Log.d(TAG, "Deleting all operations")
            syncOutboxDao.deleteAllOperations()
            Log.d(TAG, "Successfully deleted all operations")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all operations", e)
            throw e
        }
    }
}