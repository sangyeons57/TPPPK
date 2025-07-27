package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ProjectsWrapperRemoteDataSource
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.repository.base.ProjectsWrapperRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.ProjectsWrapperRepositoryFactoryContext
import javax.inject.Inject

/**
 * Remote ProjectsWrapper Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class ProjectsWrapperRepositoryImpl @Inject constructor(
    private val projectsWrapperRemoteDataSource: ProjectsWrapperRemoteDataSource,
    override val factoryContext: ProjectsWrapperRepositoryFactoryContext
) : ProjectsWrapperRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        userId: String?
    ): CustomResult<SyncResult<ProjectsWrapper>, Exception> {
        return projectsWrapperRemoteDataSource.syncFromServer(lastSyncCursor, userId)
    }

    override suspend fun syncToServer(
        userId: String?
    ): CustomResult<Int, Exception> {
        return projectsWrapperRemoteDataSource.syncToServer(userId)
    }

    override suspend fun forceSyncAll(
        userId: String?
    ): CustomResult<Int, Exception> {
        return projectsWrapperRemoteDataSource.forceSyncAll(userId)
    }

    override suspend fun resolveConflicts(
        conflictedWrapperIds: List<String>
    ): CustomResult<Int, Exception> {
        return projectsWrapperRemoteDataSource.resolveConflicts(conflictedWrapperIds)
    }
}
