package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.repository.factory.context.ScheduleRepositoryFactoryContext
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import java.time.LocalDate
import java.time.YearMonth

/**
 * Remote Schedule Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
interface ScheduleRepository {
    val factoryContext: ScheduleRepositoryFactoryContext

    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        userId: String? = null,
        projectId: String? = null
    ): CustomResult<SyncResult<Schedule>, Exception>

    suspend fun syncToServer(
        userId: String? = null,
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun forceSyncAll(
        userId: String? = null,
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun resolveConflicts(
        conflictedScheduleIds: List<String>
    ): CustomResult<Int, Exception>

    // === Firebase Functions (서버 작업) ===
    
    suspend fun findByDateSummaryForMonth(userId: UserId, yearMonth: YearMonth): CustomResult<Set<LocalDate>, Exception>
}
