package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ScheduleRemoteDataSource
import com.example.domain.model.base.Schedule
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.ScheduleRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.ScheduleRepositoryFactoryContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Remote Schedule Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleRemoteDataSource: ScheduleRemoteDataSource,
    override val factoryContext: ScheduleRepositoryFactoryContext,
) : ScheduleRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        userId: String?,
        projectId: String?
    ): CustomResult<SyncResult<Schedule>, Exception> {
        return scheduleRemoteDataSource.syncFromServer(lastSyncCursor, userId, projectId)
    }

    override suspend fun syncToServer(
        userId: String?,
        projectId: String?
    ): CustomResult<Int, Exception> {
        return scheduleRemoteDataSource.syncToServer(userId, projectId)
    }

    override suspend fun forceSyncAll(
        userId: String?,
        projectId: String?
    ): CustomResult<Int, Exception> {
        return scheduleRemoteDataSource.forceSyncAll(userId, projectId)
    }

    override suspend fun resolveConflicts(
        conflictedScheduleIds: List<String>
    ): CustomResult<Int, Exception> {
        return scheduleRemoteDataSource.resolveConflicts(conflictedScheduleIds)
    }

    // === Firebase Functions (서버 작업) ===
    
    override suspend fun findByDateSummaryForMonth(
        userId: UserId,
        yearMonth: YearMonth
    ): CustomResult<Set<LocalDate>, Exception> {
        return scheduleRemoteDataSource.findDateSummaryForMonth(userId.value, yearMonth)
    }
}
