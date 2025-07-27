package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ProjectChannelRemoteDataSource
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.domain.model.base.ProjectChannel
import com.example.domain.repository.base.ProjectChannelRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.ProjectChannelRepositoryFactoryContext
import javax.inject.Inject

/**
 * Remote ProjectChannel Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class ProjectChannelRepositoryImpl @Inject constructor(
    private val projectChannelRemoteDataSource: ProjectChannelRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
    override val factoryContext: ProjectChannelRepositoryFactoryContext
) : ProjectChannelRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        projectId: String?
    ): CustomResult<SyncResult<ProjectChannel>, Exception> {
        return projectChannelRemoteDataSource.syncFromServer(lastSyncCursor, projectId)
    }

    override suspend fun syncToServer(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return projectChannelRemoteDataSource.syncToServer(projectId)
    }

    override suspend fun forceSyncAll(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return projectChannelRemoteDataSource.forceSyncAll(projectId)
    }

    override suspend fun resolveConflicts(
        conflictedChannelIds: List<String>
    ): CustomResult<Int, Exception> {
        return projectChannelRemoteDataSource.resolveConflicts(conflictedChannelIds)
    }

    // === Firebase Functions (서버 작업) ===

    override suspend fun createChannel(
        projectId: String,
        name: String,
        description: String?,
        categoryId: String?
    ): CustomResult<ProjectChannel, Exception> {
        return functionsRemoteDataSource.createChannel(projectId, name, description, categoryId)
    }

    override suspend fun updateChannel(
        channelId: String,
        name: String?,
        description: String?,
        categoryId: String?
    ): CustomResult<Unit, Exception> {
        return functionsRemoteDataSource.updateChannel(channelId, name, description, categoryId)
    }

    override suspend fun deleteChannel(channelId: String): CustomResult<Unit, Exception> {
        return functionsRemoteDataSource.deleteChannel(channelId)
    }
}
