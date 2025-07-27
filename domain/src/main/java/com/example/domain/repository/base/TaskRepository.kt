package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.repository.factory.context.TaskRepositoryFactoryContext

/**
 * Remote Task Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
interface TaskRepository {
    val factoryContext: TaskRepositoryFactoryContext

    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        projectId: String? = null,
        userId: String? = null
    ): CustomResult<SyncResult<Task>, Exception>

    suspend fun syncToServer(
        projectId: String? = null,
        userId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun forceSyncAll(
        projectId: String? = null,
        userId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun resolveConflicts(
        conflictedTaskIds: List<String>
    ): CustomResult<Int, Exception>
}