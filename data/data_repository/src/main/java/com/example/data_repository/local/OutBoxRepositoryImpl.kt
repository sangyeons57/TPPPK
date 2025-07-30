package com.example.data_repository.local

import com.example.core_common.result.CustomResult
import com.example.data_datasource.local.OutBoxDataSource
import com.example.domain.model.enum.OutBoxStatus
import com.example.domain.model.sync.OutBox
import com.example.domain_repository.local.OutBoxRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OutBox 관련 데이터 접근을 담당하는 Repository 구현체
 * Clean Architecture 원칙에 따라 DataSource를 통해 데이터에 접근
 *
 * Repository는 비즈니스 로직 처리 및 DataSource 조합을 담당하며,
 * 실제 데이터 변환 및 DAO 접근은 DataSource에서 처리
 */
@Singleton
class OutBoxRepositoryImpl @Inject constructor(
    private val outBoxDataSource: OutBoxDataSource
) : OutBoxRepository {


    // ================================
    // Domain Repository Interface 구현
    // ================================

    override suspend fun insert(outBox: OutBox<*>): CustomResult<Unit, Exception> {
        return outBoxDataSource.insert(outBox)
    }

    override suspend fun insertAll(outBoxes: List<OutBox<*>>): CustomResult<Unit, Exception> {
        return outBoxDataSource.insertAll(outBoxes)
    }

    override suspend fun update(outBox: OutBox<*>): CustomResult<Unit, Exception> {
        return outBoxDataSource.update(outBox)
    }

    override suspend fun getById(id: String): CustomResult<OutBox<*>?, Exception> {
        return outBoxDataSource.getById(id)
    }

    override suspend fun getByStatus(status: OutBoxStatus): CustomResult<List<OutBox<*>>, Exception> {
        return outBoxDataSource.getByStatus(status)
    }

    override suspend fun getByStatuses(statuses: List<OutBoxStatus>): CustomResult<List<OutBox<*>>, Exception> {
        return outBoxDataSource.getByStatuses(statuses)
    }

    override suspend fun getByEntityTypeAndId(
        entityType: String,
        entityId: String
    ): CustomResult<List<OutBox<*>>, Exception> {
        return outBoxDataSource.getByEntityTypeAndId(entityType, entityId)
    }

    override suspend fun getPendingOperations(limit: Int): CustomResult<List<OutBox<*>>, Exception> {
        return outBoxDataSource.getPendingOperations(limit)
    }

    override suspend fun updateStatusByIds(
        ids: List<String>,
        newStatus: OutBoxStatus
    ): CustomResult<Int, Exception> {
        return outBoxDataSource.updateStatusByIds(ids, newStatus)
    }

    override suspend fun resetRetryableFailedOperations(): CustomResult<Int, Exception> {
        return outBoxDataSource.resetRetryableFailedOperations()
    }

    override suspend fun deleteCompleted(): CustomResult<Int, Exception> {
        return outBoxDataSource.deleteCompleted()
    }

    override suspend fun delete(outBox: OutBox<*>): CustomResult<Unit, Exception> {
        return outBoxDataSource.delete(outBox)
    }

    override suspend fun deleteExpiredOperations(timeoutMs: Long): CustomResult<Int, Exception> {
        return outBoxDataSource.deleteExpiredOperations(timeoutMs)
    }

    override suspend fun getCountByStatus(status: OutBoxStatus): CustomResult<Int, Exception> {
        return outBoxDataSource.getCountByStatus(status)
    }

    override suspend fun getTotalCount(): CustomResult<Int, Exception> {
        return outBoxDataSource.getTotalCount()
    }

    override suspend fun getStatusStatistics(): CustomResult<Map<OutBoxStatus, Int>, Exception> {
        return outBoxDataSource.getStatusStatistics()
    }

    override suspend fun getAllForDebug(): CustomResult<List<OutBox<*>>, Exception> {
        return outBoxDataSource.getAllForDebug()
    }

    override suspend fun deleteAll(): CustomResult<Unit, Exception> {
        return outBoxDataSource.deleteAll()
    }

}