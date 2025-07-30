package com.example.data_datasource.local

import com.example.core_common.result.CustomResult
import com.example.data_datasource.dao.ScopeMetadataDao
import com.example.data_datasource.local.SyncStatistics
import com.example.data_model.local.ScopeMetadataEntity
import com.example.domain.model.sync.ScopeMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ScopeMetadata 로컬 데이터 소스 구현체
 * ScopeMetadataDao를 래핑하고 Entity ↔ Domain Model 변환 처리
 */
@Singleton
class ScopeMetadataDataSourceImpl @Inject constructor(
    private val scopeMetadataDao: ScopeMetadataDao
) : ScopeMetadataDataSource {

    // ================================
    // 기본 CRUD 작업
    // ================================
    
    override suspend fun save(scopeMetadata: ScopeMetadata): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = ScopeMetadataEntity.fromDomainModel(scopeMetadata)
                scopeMetadataDao.insert(entity)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun saveAll(scopeMetadataList: List<ScopeMetadata>): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = scopeMetadataList.map { ScopeMetadataEntity.fromDomainModel(it) }
                scopeMetadataDao.insertAll(entities)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun delete(scopeMetadata: ScopeMetadata): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = ScopeMetadataEntity.fromDomainModel(scopeMetadata)
                scopeMetadataDao.delete(entity)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 키-값 조회
    // ================================
    
    override suspend fun getByKey(key: String): CustomResult<ScopeMetadata?, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = scopeMetadataDao.getByKey(key)
                val domainModel = entity?.toDomainModel()
                CustomResult.Success(domainModel)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getAll(): CustomResult<List<ScopeMetadata>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = scopeMetadataDao.getAll()
                val domainModels = entities.map { it.toDomainModel() }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getByKeyPattern(keyPattern: String): CustomResult<List<ScopeMetadata>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = scopeMetadataDao.getByKeyPattern(keyPattern)
                val domainModels = entities.map { it.toDomainModel() }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getSyncedAfter(afterTimestampMs: Long): CustomResult<List<ScopeMetadata>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = scopeMetadataDao.getSyncedAfter(afterTimestampMs)
                val domainModels = entities.map { it.toDomainModel() }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun keyExists(key: String): CustomResult<Boolean, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val exists = scopeMetadataDao.keyExists(key)
                CustomResult.Success(exists)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 키 관리
    // ================================
    
    override suspend fun deleteByKey(key: String): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val deletedCount = scopeMetadataDao.deleteByKey(key)
                CustomResult.Success(deletedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun deleteByKeyPattern(keyPattern: String): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val deletedCount = scopeMetadataDao.deleteByKeyPattern(keyPattern)
                CustomResult.Success(deletedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 동기화 상태 관리
    // ================================
    
    override suspend fun getErrorMetadata(minErrorCount: Long): CustomResult<List<ScopeMetadata>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = scopeMetadataDao.getErrorMetadata(minErrorCount)
                val domainModels = entities.map { it.toDomainModel() }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getActiveSyncMetadata(minSyncCount: Long, afterTimestampMs: Long): CustomResult<List<ScopeMetadata>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = scopeMetadataDao.getActiveSyncMetadata(minSyncCount, afterTimestampMs)
                val domainModels = entities.map { it.toDomainModel() }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun resetAllErrorCounts(): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val updatedCount = scopeMetadataDao.resetAllErrorCounts()
                CustomResult.Success(updatedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 정리 및 관리
    // ================================
    
    override suspend fun getTotalCount(): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val count = scopeMetadataDao.getTotalCount()
                CustomResult.Success(count)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun deleteOldMetadata(beforeTimestampMs: Long): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val deletedCount = scopeMetadataDao.deleteOldMetadata(beforeTimestampMs)
                CustomResult.Success(deletedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun deleteAll(): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                scopeMetadataDao.deleteAll()
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 통계 및 모니터링
    // ================================
    
    override suspend fun getSyncStatistics(): CustomResult<SyncStatistics, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val stat = scopeMetadataDao.getSyncStatistics()
                val syncStatistics = SyncStatistics(
                    totalCount = stat.totalCount,
                    totalSyncCount = stat.totalSyncCount,
                    avgSyncCount = stat.avgSyncCount,
                    totalErrorCount = stat.totalErrorCount,
                    avgErrorCount = stat.avgErrorCount,
                    lastSyncTimeMs = stat.lastSyncTime
                )
                CustomResult.Success(syncStatistics)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // 테스트 및 디버깅용
    // ================================
    
    override suspend fun getAllForDebug(): CustomResult<List<ScopeMetadata>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = scopeMetadataDao.getAllForDebug()
                val domainModels = entities.map { it.toDomainModel() }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
}