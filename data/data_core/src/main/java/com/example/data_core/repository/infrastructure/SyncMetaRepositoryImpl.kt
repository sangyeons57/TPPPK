import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.SyncMetadataDataSource
import com.example.data_model.local.SyncMetadataEntity
import com.example.domain.repository.infrastructure.SyncCollectionStatistics
import com.example.domain.repository.infrastructure.SyncMetaRepository
import javax.inject.Inject
import javax.inject.Singleton

ut
package com.example.data_core.repository.infrastructure

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.SyncMetadataDataSource
import com.example.data_model.local.SyncMetadataEntity
import com.example.domain.repository.infrastructure.SyncMetaRepository
import com.example.domain.repository.infrastructure.SyncCollectionStatistics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SyncMetaRepository 구현체
 * SyncMetadataDataSource를 통해 컬렉션별 동기화 메타데이터 관리
 *
 * 🚀 Production Features:
 * - 클럭 스큐 자동 측정 및 보정
 * - 동기화 상태 모니터링 및 헬스체크
 * - 적응형 동기화 간격 조정
 * - 성능 통계 수집 및 분석
 *
 * 📋 아키텍처:
 * - Repository → DataSource → DAO 패턴 준수
 * - 모든 DataSource 호출을 래핑하여 비즈니스 로직 처리
 * - 예외 처리 및 로깅으로 안정성 보장
 */
@Singleton
class SyncMetaRepositoryImpl @Inject constructor(
    private val syncMetadataDataSource: SyncMetadataDataSource
) : SyncMetaRepository {

    companion object {
        private const val TAG = "SyncMetaRepository"
        private const val DEFAULT_SYNC_AGE_MS = 3600000L // 1시간
        private const val CLOCK_SKEW_REFRESH_INTERVAL_MS = 3600000L // 1시간
        private const val MAX_ACCEPTABLE_CLOCK_SKEW_MS = 60000L // 1분
    }

    // === 동기화 커서 관리 ===

    override suspend fun getLastSyncCursor(collectionName: String): CustomResult<Long?, Exception> {
        return try {
            Log.d(TAG, "Getting last sync cursor for collection: $collectionName")

            val cursor = syncMetadataDataSource.getLastSyncCursor(collectionName)

            Log.d(TAG, "Retrieved sync cursor for $collectionName: $cursor")
            CustomResult.Success(cursor)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last sync cursor for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateSyncCursor(
        collectionName: String,
        cursor: Long,
        timestamp: Long
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(
                TAG,
                "Updating sync cursor for $collectionName: cursor=$cursor, timestamp=$timestamp"
            )

            syncMetadataDataSource.updateSyncCursor(collectionName, cursor, timestamp)

            Log.d(TAG, "Successfully updated sync cursor for $collectionName")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update sync cursor for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun getLastSuccessfulSyncTime(collectionName: String): CustomResult<Long?, Exception> {
        return try {
            Log.d(TAG, "Getting last successful sync time for collection: $collectionName")

            val timestamp = syncMetadataDataSource.getLastSuccessfulSyncTime(collectionName)

            Log.d(TAG, "Retrieved last successful sync time for $collectionName: $timestamp")
            CustomResult.Success(timestamp)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last successful sync time for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    // === 클럭 스큐 관리 ===

    override suspend fun measureAndStoreClockSkew(
        collectionName: String,
        serverTime: Long,
        clientTime: Long
    ): CustomResult<Long, Exception> {
        return try {
            Log.d(
                TAG,
                "Measuring clock skew for $collectionName: server=$serverTime, client=$clientTime"
            )

            val clockSkew = serverTime - clientTime
            val measuredAt = System.currentTimeMillis()

            // 기존 메타데이터 조회 후 업데이트
            val existingMetadata = syncMetadataDataSource.getSyncMetadata(collectionName)
            val updatedMetadata = if (existingMetadata != null) {
                existingMetadata.copy(
                    clockSkew = clockSkew,
                    clockSkewMeasuredAt = measuredAt
                )
            } else {
                SyncMetadataEntity.createInitial(collectionName).copy(
                    clockSkew = clockSkew,
                    clockSkewMeasuredAt = measuredAt
                )
            }

            syncMetadataDataSource.saveSyncMetadata(updatedMetadata)

            Log.d(
                TAG,
                "Successfully measured and stored clock skew for $collectionName: ${clockSkew}ms"
            )
            CustomResult.Success(clockSkew)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to measure clock skew for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun getClockSkew(collectionName: String): CustomResult<Long?, Exception> {
        return try {
            val metadata = syncMetadataDataSource.getSyncMetadata(collectionName)
            val clockSkew = metadata?.clockSkew

            Log.d(TAG, "Retrieved clock skew for $collectionName: $clockSkew")
            CustomResult.Success(clockSkew)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get clock skew for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun adjustClientTime(
        collectionName: String,
        clientTime: Long
    ): CustomResult<Long, Exception> {
        return try {
            val metadata = syncMetadataDataSource.getSyncMetadata(collectionName)
            val adjustedTime = SyncMetadataEntity.adjustClientTime(clientTime, metadata?.clockSkew)

            Log.d(TAG, "Adjusted client time for $collectionName: $clientTime -> $adjustedTime")
            CustomResult.Success(adjustedTime)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust client time for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun needsClockSkewMeasurement(collectionName: String): CustomResult<Boolean, Exception> {
        return try {
            val metadata = syncMetadataDataSource.getSyncMetadata(collectionName)
            val needsMeasurement =
                metadata?.let { SyncMetadataEntity.isClockSkewOutdated(it) } ?: true

            Log.d(TAG, "Clock skew measurement needed for $collectionName: $needsMeasurement")
            CustomResult.Success(needsMeasurement)

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to check clock skew measurement need for collection: $collectionName",
                e
            )
            CustomResult.Failure(e)
        }
    }

    // === 동기화 상태 관리 ===

    override suspend fun recordSyncSuccess(
        collectionName: String,
        syncDurationMs: Long
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Recording sync success for $collectionName: duration=${syncDurationMs}ms")

            val existingMetadata = syncMetadataDataSource.getSyncMetadata(collectionName)
                ?: SyncMetadataEntity.createInitial(collectionName)

            val newAverageDuration = calculateNewAverage(
                existingMetadata.averageSyncDurationMs,
                syncDurationMs,
                existingMetadata.consecutiveSuccessCount
            )

            val updatedMetadata = existingMetadata.copy(
                lastSuccessfulSync = System.currentTimeMillis(),
                consecutiveSuccessCount = existingMetadata.consecutiveSuccessCount + 1,
                consecutiveFailureCount = 0, // 성공시 실패 카운터 리셋
                averageSyncDurationMs = newAverageDuration
            )

            syncMetadataDataSource.saveSyncMetadata(updatedMetadata)

            Log.d(TAG, "Successfully recorded sync success for $collectionName")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to record sync success for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun recordSyncFailure(
        collectionName: String,
        errorMessage: String
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Recording sync failure for $collectionName: $errorMessage")

            val existingMetadata = syncMetadataDataSource.getSyncMetadata(collectionName)
                ?: SyncMetadataEntity.createInitial(collectionName)

            val updatedMetadata = existingMetadata.copy(
                consecutiveSuccessCount = 0, // 실패시 성공 카운터 리셋
                consecutiveFailureCount = existingMetadata.consecutiveFailureCount + 1
            )

            syncMetadataDataSource.saveSyncMetadata(updatedMetadata)

            Log.d(TAG, "Successfully recorded sync failure for $collectionName")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to record sync failure for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun isSyncHealthy(collectionName: String): CustomResult<Boolean, Exception> {
        return try {
            val metadata = syncMetadataDataSource.getSyncMetadata(collectionName)
            val isHealthy = metadata?.isSyncHealthy() ?: true

            Log.d(TAG, "Sync health for $collectionName: $isHealthy")
            CustomResult.Success(isHealthy)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to check sync health for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun getConsecutiveFailureCount(collectionName: String): CustomResult<Int, Exception> {
        return try {
            val metadata = syncMetadataDataSource.getSyncMetadata(collectionName)
            val failureCount = metadata?.consecutiveFailureCount ?: 0

            Log.d(TAG, "Consecutive failure count for $collectionName: $failureCount")
            CustomResult.Success(failureCount)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get consecutive failure count for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 필요 판단 ===

    override suspend fun getCollectionsNeedingSync(maxAgeMs: Long): CustomResult<List<String>, Exception> {
        return try {
            Log.d(TAG, "Getting collections needing sync (maxAge: ${maxAgeMs}ms)")

            val allMetadata = syncMetadataDataSource.getAllSyncMetadata()
            val currentTime = System.currentTimeMillis()

            val needingSyncCollections = allMetadata.filter { metadata ->
                val age = currentTime - metadata.lastSuccessfulSync
                age > maxAgeMs
            }.map { it.collectionName }

            Log.d(TAG, "Found ${needingSyncCollections.size} collections needing sync")
            CustomResult.Success(needingSyncCollections)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get collections needing sync", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun needsSync(
        collectionName: String,
        maxAgeMs: Long
    ): CustomResult<Boolean, Exception> {
        return try {
            val metadata = syncMetadataDataSource.getSyncMetadata(collectionName)
            val needsSync = if (metadata != null) {
                val age = System.currentTimeMillis() - metadata.lastSuccessfulSync
                age > maxAgeMs
            } else {
                true // 메타데이터가 없으면 동기화 필요
            }

            Log.d(TAG, "Sync needed for $collectionName: $needsSync")
            CustomResult.Success(needsSync)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to check sync need for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    // === 메타데이터 초기화 및 관리 ===

    override suspend fun initializeSyncMetadata(collectionName: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Initializing sync metadata for collection: $collectionName")

            val initialMetadata = SyncMetadataEntity.createInitial(collectionName)
            syncMetadataDataSource.saveSyncMetadata(initialMetadata)

            Log.d(TAG, "Successfully initialized sync metadata for $collectionName")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize sync metadata for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun hasSyncMetadata(collectionName: String): CustomResult<Boolean, Exception> {
        return try {
            val metadata = syncMetadataDataSource.getSyncMetadata(collectionName)
            val hasMetadata = metadata != null

            Log.d(TAG, "Sync metadata exists for $collectionName: $hasMetadata")
            CustomResult.Success(hasMetadata)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to check sync metadata existence for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteSyncMetadata(collectionName: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Deleting sync metadata for collection: $collectionName")

            syncMetadataDataSource.deleteSyncMetadata(collectionName)

            Log.d(TAG, "Successfully deleted sync metadata for $collectionName")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete sync metadata for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun clearAllSyncMetadata(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Clearing all sync metadata")

            syncMetadataDataSource.deleteAllSyncMetadata()

            Log.d(TAG, "Successfully cleared all sync metadata")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all sync metadata", e)
            CustomResult.Failure(e)
        }
    }

    // === 통계 및 모니터링 ===

    override suspend fun getAllSyncStatistics(): CustomResult<List<SyncCollectionStatistics>, Exception> {
        return try {
            Log.d(TAG, "Getting all sync statistics")

            val allMetadata = syncMetadataDataSource.getAllSyncMetadata()
            val statistics = allMetadata.map { it.toStatistics() }

            Log.d(TAG, "Retrieved statistics for ${statistics.size} collections")
            CustomResult.Success(statistics)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all sync statistics", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun getSyncStatistics(collectionName: String): CustomResult<SyncCollectionStatistics?, Exception> {
        return try {
            val metadata = syncMetadataDataSource.getSyncMetadata(collectionName)
            val statistics = metadata?.toStatistics()

            Log.d(TAG, "Retrieved statistics for $collectionName: ${statistics != null}")
            CustomResult.Success(statistics)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get sync statistics for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun getAverageSyncDuration(collectionName: String): CustomResult<Long?, Exception> {
        return try {
            val metadata = syncMetadataDataSource.getSyncMetadata(collectionName)
            val averageDuration = metadata?.averageSyncDurationMs

            Log.d(TAG, "Average sync duration for $collectionName: $averageDuration")
            CustomResult.Success(averageDuration)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get average sync duration for collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    // === 컬렉션 관리 ===

    override suspend fun getAllRegisteredCollections(): CustomResult<List<String>, Exception> {
        return try {
            val allMetadata = syncMetadataDataSource.getAllSyncMetadata()
            val collections = allMetadata.map { it.collectionName }

            Log.d(TAG, "Found ${collections.size} registered collections")
            CustomResult.Success(collections)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all registered collections", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun registerCollection(collectionName: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Registering collection: $collectionName")

            // 이미 등록된 컬렉션인지 확인
            val existingMetadata = syncMetadataDataSource.getSyncMetadata(collectionName)
            if (existingMetadata == null) {
                initializeSyncMetadata(collectionName)
                Log.d(TAG, "Successfully registered new collection: $collectionName")
            } else {
                Log.d(TAG, "Collection already registered: $collectionName")
            }

            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to register collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun unregisterCollection(collectionName: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "Unregistering collection: $collectionName")

            deleteSyncMetadata(collectionName)

            Log.d(TAG, "Successfully unregistered collection: $collectionName")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister collection: $collectionName", e)
            CustomResult.Failure(e)
        }
    }

    // === 헬퍼 메서드 ===

    /**
     * 새로운 평균값 계산 (점진적 평균)
     */
    private fun calculateNewAverage(
        currentAverage: Long?,
        newValue: Long,
        sampleCount: Int
    ): Long {
        return if (currentAverage == null || sampleCount == 0) {
            newValue
        } else {
            // 점진적 평균: new_avg = old_avg + (new_value - old_avg) / (count + 1)
            currentAverage + (newValue - currentAverage) / (sampleCount + 1)
        }
    }

    /**
     * SyncMetadataEntity를 SyncCollectionStatistics로 변환
     */
    private fun SyncMetadataEntity.toStatistics(): SyncCollectionStatistics {
        val currentTime = System.currentTimeMillis()
        val needsSync = (currentTime - this.lastSuccessfulSync) > DEFAULT_SYNC_AGE_MS

        return SyncCollectionStatistics(
            collectionName = this.collectionName,
            lastSyncCursor = this.lastServerCursor,
            lastSuccessfulSync = this.lastSuccessfulSync,
            clockSkew = this.clockSkew,
            clockSkewMeasuredAt = this.clockSkewMeasuredAt,
            consecutiveSuccessCount = this.consecutiveSuccessCount,
            consecutiveFailureCount = this.consecutiveFailureCount,
            averageSyncDurationMs = this.averageSyncDurationMs,
            isHealthy = this.isSyncHealthy(),
            needsSync = needsSync,
            hasAcceptableClockSkew = this.hasAcceptableClockSkew()
        )
    }
}