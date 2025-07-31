package com.example.data_repository.local

import com.example.core_common.result.CustomResult
import com.example.data_datasource.local.ScopeMetadataDataSource
import com.example.data_datasource.local.SyncStatistics
import com.example.domain.model.sync.ScopeMetadata
import com.example.domain_repository.local.ScopeMetaDataRepository
import com.example.domain_repository.local.SyncStatisticsData
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ScopeMetadata 관련 데이터 접근을 담당하는 Repository 구현체
 * Clean Architecture 원칙에 따라 DataSource를 통해 데이터에 접근
 *
 * Repository는 비즈니스 로직 처리 및 DataSource 조합을 담당하며,
 * 실제 데이터 변환 및 DAO 접근은 DataSource에서 처리
 */
@Singleton
class ScopeMetaDataRepositoryImpl @Inject constructor(
    private val scopeMetadataDataSource: ScopeMetadataDataSource
) : ScopeMetaDataRepository {

    // ================================
    // SyncManager용 - 기본 CRUD 작업
    // ================================

    override suspend fun save(scopeMetadata: ScopeMetadata): CustomResult<Unit, Exception> {
        return scopeMetadataDataSource.save(scopeMetadata)
    }

    override suspend fun saveAll(scopeMetadataList: List<ScopeMetadata>): CustomResult<Unit, Exception> {
        return scopeMetadataDataSource.saveAll(scopeMetadataList)
    }

    override suspend fun delete(scopeMetadata: ScopeMetadata): CustomResult<Unit, Exception> {
        return scopeMetadataDataSource.delete(scopeMetadata)
    }

    // ================================
    // SyncManager용 - 키-값 조회
    // ================================

    override suspend fun findByKey(key: String): CustomResult<ScopeMetadata?, Exception> {
        return scopeMetadataDataSource.getByKey(key)
    }

    override suspend fun findAll(): CustomResult<List<ScopeMetadata>, Exception> {
        return scopeMetadataDataSource.getAll()
    }

    override suspend fun findByKeyPattern(keyPattern: String): CustomResult<List<ScopeMetadata>, Exception> {
        return scopeMetadataDataSource.getByKeyPattern(keyPattern)
    }

    override suspend fun findSyncedAfter(afterTimestamp: Instant): CustomResult<List<ScopeMetadata>, Exception> {
        return scopeMetadataDataSource.getSyncedAfter(afterTimestamp.toEpochMilli())
    }

    override suspend fun existsByKey(key: String): CustomResult<Boolean, Exception> {
        return scopeMetadataDataSource.keyExists(key)
    }

    // ================================
    // SyncManager용 - 키 관리
    // ================================

    override suspend fun deleteByKey(key: String): CustomResult<Int, Exception> {
        return scopeMetadataDataSource.deleteByKey(key)
    }

    override suspend fun deleteByKeyPattern(keyPattern: String): CustomResult<Int, Exception> {
        return scopeMetadataDataSource.deleteByKeyPattern(keyPattern)
    }

    // ================================
    // SyncManager용 - 동기화 상태 관리
    // ================================

    override suspend fun findErrorMetadata(minErrorCount: Long): CustomResult<List<ScopeMetadata>, Exception> {
        return scopeMetadataDataSource.getErrorMetadata(minErrorCount)
    }

    override suspend fun findActiveSyncMetadata(
        minSyncCount: Long,
        afterTimestamp: Instant
    ): CustomResult<List<ScopeMetadata>, Exception> {
        return scopeMetadataDataSource.getActiveSyncMetadata(
            minSyncCount,
            afterTimestamp.toEpochMilli()
        )
    }

    override suspend fun resetAllErrorCounts(): CustomResult<Int, Exception> {
        return scopeMetadataDataSource.resetAllErrorCounts()
    }

    // ================================
    // SyncManager용 - 정리 및 관리
    // ================================

    override suspend fun count(): CustomResult<Int, Exception> {
        return scopeMetadataDataSource.getTotalCount()
    }

    override suspend fun deleteOldMetadata(beforeTimestamp: Instant): CustomResult<Int, Exception> {
        return scopeMetadataDataSource.deleteOldMetadata(beforeTimestamp.toEpochMilli())
    }

    override suspend fun deleteAll(): CustomResult<Unit, Exception> {
        return scopeMetadataDataSource.deleteAll()
    }

    // ================================
    // SyncManager용 - 통계 및 모니터링
    // ================================

    override suspend fun getSyncStatistics(): CustomResult<SyncStatisticsData, Exception> {
        return when (val result = scopeMetadataDataSource.getSyncStatistics()) {
            is CustomResult.Success -> {
                val dataSourceStats = result.data
                val domainStats = SyncStatisticsData(
                    totalCount = dataSourceStats.totalCount,
                    totalSyncCount = dataSourceStats.totalSyncCount,
                    avgSyncCount = dataSourceStats.avgSyncCount,
                    totalErrorCount = dataSourceStats.totalErrorCount,
                    avgErrorCount = dataSourceStats.avgErrorCount,
                    lastSyncTime = dataSourceStats.lastSyncTimeMs?.let { Instant.ofEpochMilli(it) }
                )
                CustomResult.Success(domainStats)
            }

            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }

    // ================================
    // 테스트 및 디버깅용
    // ================================

    override suspend fun findAllForDebug(): CustomResult<List<ScopeMetadata>, Exception> {
        return scopeMetadataDataSource.getAllForDebug()
    }
}