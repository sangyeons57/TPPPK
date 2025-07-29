package com.example.data_core.datasource.local

import com.example.core_common.result.CustomResult
import com.example.data_core.dao.OutBoxDao
import com.example.data_model.local.OutBoxEntity
import com.example.domain.model.enum.OutBoxStatus
import com.example.domain.model.sync.OutBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OutBox 로컬 데이터 소스 구현체
 * OutBoxDao를 래핑하고 Entity ↔ Domain Model 변환 처리
 */
@Singleton
class OutBoxDataSourceImpl @Inject constructor(
    private val outBoxDao: OutBoxDao
) : OutBoxDataSource {

    // ================================
    // Repository용 - 트랜잭션 내 OutBox 추가
    // ================================
    
    override suspend fun insert(outBox: OutBox): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = OutBoxEntity.fromDomainModel(outBox)
                outBoxDao.insert(entity)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun insertAll(outBoxes: List<OutBox>): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = outBoxes.map { OutBoxEntity.fromDomainModel(it) }
                outBoxDao.insertAll(entities)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 도메인 모델 처리
    // ================================
    
    override suspend fun getById(id: String): CustomResult<OutBox?, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = outBoxDao.getById(id)
                val domainModel = entity?.toDomainModel()
                CustomResult.Success(domainModel)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getByStatus(status: OutBoxStatus): CustomResult<List<OutBox>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = outBoxDao.getByStatus(status.name)
                val domainModels = entities.map { it.toDomainModel() }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getByStatuses(statuses: List<OutBoxStatus>): CustomResult<List<OutBox>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val statusNames = statuses.map { it.name }
                val entities = outBoxDao.getByStatuses(statusNames)
                val domainModels = entities.map { it.toDomainModel() }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getByEntityTypeAndId(entityType: String, entityId: String): CustomResult<List<OutBox>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = outBoxDao.getByEntityTypeAndId(entityType, entityId)
                val domainModels = entities.map { it.toDomainModel() }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 배치 처리
    // ================================
    
    override suspend fun getPendingOperations(limit: Int): CustomResult<List<OutBox>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = outBoxDao.getPendingOperations(limit)
                val domainModels = entities.map { it.toDomainModel() }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun update(outBox: OutBox): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = OutBoxEntity.fromDomainModel(outBox)
                outBoxDao.update(entity)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun updateStatusByIds(ids: List<String>, newStatus: OutBoxStatus): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                if (ids.isEmpty()) {
                    return@withContext CustomResult.Success(0)
                }
                val updatedCount = outBoxDao.updateStatusByIds(ids, newStatus.name)
                CustomResult.Success(updatedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun resetRetryableFailedOperations(): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val resetCount = outBoxDao.resetRetryableFailedOperations()
                CustomResult.Success(resetCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 정리 및 관리
    // ================================
    
    override suspend fun deleteCompleted(): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val deletedCount = outBoxDao.deleteCompleted()
                CustomResult.Success(deletedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun delete(outBox: OutBox): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = OutBoxEntity.fromDomainModel(outBox)
                outBoxDao.delete(entity)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun deleteExpiredOperations(timeoutMs: Long): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val currentTimeMs = System.currentTimeMillis()
                val deletedCount = outBoxDao.deleteExpiredOperations(currentTimeMs, timeoutMs)
                CustomResult.Success(deletedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 통계 및 모니터링
    // ================================
    
    override suspend fun getCountByStatus(status: OutBoxStatus): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val count = outBoxDao.getCountByStatus(status.name)
                CustomResult.Success(count)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getTotalCount(): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val count = outBoxDao.getTotalCount()
                CustomResult.Success(count)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getStatusStatistics(): CustomResult<Map<OutBoxStatus, Int>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val statistics = outBoxDao.getStatusStatistics()
                val statusMap = statistics.associate { stat ->
                    OutBoxStatus.valueOf(stat.status) to stat.count
                }
                CustomResult.Success(statusMap)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // 테스트 및 디버깅용
    // ================================
    
    override suspend fun getAllForDebug(): CustomResult<List<OutBox>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = outBoxDao.getAllForDebug()
                val domainModels = entities.map { it.toDomainModel() }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun deleteAll(): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                outBoxDao.deleteAll()
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
}