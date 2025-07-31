package com.example.domain_usecase.sync

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.OutBoxStatus
import com.example.domain.model.sync.OutBox
import com.example.domain.model.sync.ScopeMetadata
import com.example.domain.util.RetryManager
import com.example.domain_repository.local.OutBoxRepository
import com.example.domain_repository.local.ScopeMetaDataRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 동기화 관리를 위한 UseCase Manager 구현체
 *
 * Clean Architecture와 SSOT 원칙에 따라 구현:
 * - Repository 패턴을 통한 데이터 접근
 * - RetryManager를 통한 안정적인 재시도 로직
 * - CustomResult를 통한 일관된 오류 처리
 * - UseCase 계층에서 여러 Repository를 조합한 복합 비즈니스 로직
 */
@Singleton
class SyncManagerImpl @Inject constructor(
    private val outBoxRepository: OutBoxRepository,
    private val scopeMetaDataRepository: ScopeMetaDataRepository,
    private val retryManager: RetryManager
) : SyncManager {

    // ================================
    // 핵심 동기화 작업
    // ================================

    override suspend fun processOutBoxQueue(batchSize: Int): CustomResult<SyncResult, Exception> {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<SyncError>()

        return try {
            // 1. 대기 중인 작업들 조회
            val pendingOperationsResult = outBoxRepository.getPendingOperations(batchSize)
            when (pendingOperationsResult) {
                is CustomResult.Success -> {
                    val pendingOperations = pendingOperationsResult.data
                    if (pendingOperations.isEmpty()) {
                        CustomResult.Success(
                            SyncResult(
                                totalProcessed = 0,
                                successCount = 0,
                                failureCount = 0,
                                skippedCount = 0,
                                processingTimeMs = System.currentTimeMillis() - startTime,
                                errors = emptyList()
                            )
                        )
                    } else {
                        // 2. 각 작업을 병렬로 처리
                        val results = coroutineScope {
                            pendingOperations.map { outBox ->
                                async { processOutBoxOperation(outBox) }
                            }.map { it.await() }
                        }

                        // 3. 결과 집계
                        var successCount = 0
                        var failureCount = 0
                        var skippedCount = 0

                        results.forEach { result ->
                            when (result) {
                                is ProcessResult.Success -> successCount++
                                is ProcessResult.Failure -> {
                                    failureCount++
                                    errors.add(result.error)
                                }

                                is ProcessResult.Skipped -> skippedCount++
                            }
                        }

                        val syncResult = SyncResult(
                            totalProcessed = pendingOperations.size,
                            successCount = successCount,
                            failureCount = failureCount,
                            skippedCount = skippedCount,
                            processingTimeMs = System.currentTimeMillis() - startTime,
                            errors = errors.toList()
                        )

                        CustomResult.Success(syncResult)
                    }
                }

                else -> CustomResult.Failure(Exception("Failed to get pending operations: $pendingOperationsResult"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateSyncScope(
        scopeKey: String,
        cursor: Instant
    ): CustomResult<Unit, Exception> {
        return try {
            // 기존 ScopeMetadata 조회 또는 새로 생성
            val existingMetadataResult = scopeMetaDataRepository.findByKey(scopeKey)
            if (existingMetadataResult is CustomResult.Success) {
                val updatedMetadata = existingMetadataResult.data?.apply {
                    updateCursor(cursor)
                } ?: ScopeMetadata.create(scopeKey, cursor)

                // ScopeMetadata 저장
                scopeMetaDataRepository.save(updatedMetadata)
            } else {
                CustomResult.Failure(Exception("Failed to get existing metadata: $existingMetadataResult"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun getSyncStatus(scopeKey: String): CustomResult<ScopeMetadata?, Exception> {
        return scopeMetaDataRepository.findByKey(scopeKey)
    }

    // ================================
    // 재시도 및 복구 작업
    // ================================

    override suspend fun resetFailedOperations(): CustomResult<Int, Exception> {
        return outBoxRepository.resetRetryableFailedOperations()
    }

    override suspend fun recordSyncError(
        scopeKey: String,
        errorMessage: String
    ): CustomResult<Unit, Exception> {
        return try {
            val existingMetadataResult = scopeMetaDataRepository.findByKey(scopeKey)
            if (existingMetadataResult is CustomResult.Success) {
                val updatedMetadata = existingMetadataResult.data?.apply {
                    recordError(errorMessage)
                } ?: ScopeMetadata.create(scopeKey).apply {
                    recordError(errorMessage)
                }

                scopeMetaDataRepository.save(updatedMetadata)
            } else {
                CustomResult.Failure(Exception("Failed to get existing metadata: $existingMetadataResult"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun resetAllErrorCounts(): CustomResult<Int, Exception> {
        return scopeMetaDataRepository.resetAllErrorCounts()
    }

    // ================================
    // 상태 조회 및 모니터링
    // ================================

    override suspend fun getSyncStatusSummary(): CustomResult<SyncStatusSummary, Exception> {
        return try {
            coroutineScope {
                val pendingCountDeferred =
                    async { outBoxRepository.getCountByStatus(OutBoxStatus.PENDING) }
                val failedCountDeferred =
                    async { outBoxRepository.getCountByStatus(OutBoxStatus.FAILED) }
                val completedCountDeferred =
                    async { outBoxRepository.getCountByStatus(OutBoxStatus.COMPLETED) }
                val scopeStatsDeferred = async { scopeMetaDataRepository.getSyncStatistics() }
                val errorScopesDeferred = async { scopeMetaDataRepository.findErrorMetadata() }

                val pendingCount =
                    (pendingCountDeferred.await() as? CustomResult.Success)?.data ?: 0
                val failedCount = (failedCountDeferred.await() as? CustomResult.Success)?.data ?: 0
                val completedCount =
                    (completedCountDeferred.await() as? CustomResult.Success)?.data ?: 0
                val scopeStats = (scopeStatsDeferred.await() as? CustomResult.Success)?.data
                val errorScopes =
                    (errorScopesDeferred.await() as? CustomResult.Success)?.data ?: emptyList()

                val summary = SyncStatusSummary(
                    pendingOperationsCount = pendingCount,
                    failedOperationsCount = failedCount,
                    completedOperationsCount = completedCount,
                    activeScopesCount = scopeStats?.totalCount ?: 0,
                    errorScopesCount = errorScopes.size,
                    totalSyncOperations = scopeStats?.totalSyncCount ?: 0L,
                    lastSyncAt = scopeStats?.lastSyncTime
                )

                CustomResult.Success(summary)
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun getErrorScopes(minErrorCount: Long): CustomResult<List<ScopeMetadata>, Exception> {
        return scopeMetaDataRepository.findErrorMetadata(minErrorCount)
    }

    // ================================
    // 정리 및 유지보수 작업
    // ================================

    override suspend fun cleanupCompletedOperations(): CustomResult<Int, Exception> {
        return outBoxRepository.deleteCompleted()
    }

    override suspend fun cleanupExpiredOperations(timeoutMs: Long): CustomResult<Int, Exception> {
        return outBoxRepository.deleteExpiredOperations(timeoutMs)
    }

    override suspend fun cleanupOldMetadata(beforeTimestamp: Instant): CustomResult<Int, Exception> {
        return scopeMetaDataRepository.deleteOldMetadata(beforeTimestamp)
    }

    // ================================
    // Private Helper Methods
    // ================================

    /**
     * 개별 OutBox 작업을 처리합니다.
     * 실제 동기화 로직은 여기에 구현되어야 하지만,
     * 현재는 스텁으로 구현되어 있습니다.
     */
    private suspend fun processOutBoxOperation(outBox: OutBox<*>): ProcessResult {
        return try {
            // TODO: 실제 동기화 로직 구현 필요
            // 예: Firebase Firestore, REST API 등으로 데이터 전송

            // 임시 성공 처리 (실제 구현 시 제거)
            val updateResult = outBoxRepository.updateStatusByIds(
                listOf(outBox.id),
                OutBoxStatus.COMPLETED
            )

            when (updateResult) {
                is CustomResult.Success -> ProcessResult.Success(outBox.id)
                else -> ProcessResult.Failure(
                    SyncError(
                        outBoxId = outBox.id,
                        entityType = outBox.entityType.name,
                        entityId = outBox.entityId,
                        errorMessage = "Failed to update status: $updateResult",
                        retryCount = outBox.getAttempts(),
                        lastAttemptAt = Instant.now()
                    )
                )
            }

        } catch (e: Exception) {
            // 실패 시 OutBox 상태를 FAILED로 변경
            outBoxRepository.updateStatusByIds(
                listOf(outBox.id),
                OutBoxStatus.FAILED
            )

            ProcessResult.Failure(
                SyncError(
                    outBoxId = outBox.id,
                    entityType = outBox.entityType.name,
                    entityId = outBox.entityId,
                    errorMessage = e.message ?: "Unknown error",
                    retryCount = outBox.getAttempts(),
                    lastAttemptAt = Instant.now()
                )
            )
        }
    }

    /**
     * 개별 OutBox 작업 처리 결과
     */
    private sealed class ProcessResult {
        data class Success(val outBoxId: String) : ProcessResult()
        data class Failure(val error: SyncError) : ProcessResult()
        data class Skipped(val reason: String) : ProcessResult()
    }
}