package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.TaskRemoteDataSource
import com.example.domain.model.base.Task
import com.example.domain.repository.base.TaskRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.TaskRepositoryFactoryContext
import javax.inject.Inject

/**
 * Remote Task Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class TaskRepositoryImpl @Inject constructor(
    private val taskRemoteDataSource: TaskRemoteDataSource,
    override val factoryContext: TaskRepositoryFactoryContext,
) : TaskRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        projectId: String?,
        userId: String?
    ): CustomResult<SyncResult<Task>, Exception> {
        return taskRemoteDataSource.syncFromServer(lastSyncCursor, projectId, userId)
    }

    override suspend fun syncToServer(
        projectId: String?,
        userId: String?
    ): CustomResult<Int, Exception> {
        return taskRemoteDataSource.syncToServer(projectId, userId)
    }

    override suspend fun forceSyncAll(
        projectId: String?,
        userId: String?
    ): CustomResult<Int, Exception> {
        return taskRemoteDataSource.forceSyncAll(projectId, userId)
    }

    override suspend fun resolveConflicts(
        conflictedTaskIds: List<String>
    ): CustomResult<Int, Exception> {
        return taskRemoteDataSource.resolveConflicts(conflictedTaskIds)
    }
}