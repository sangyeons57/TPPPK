package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.repository.factory.context.ProjectChannelRepositoryFactoryContext

/**
 * Remote ProjectChannel Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
interface ProjectChannelRepository {
    val factoryContext: ProjectChannelRepositoryFactoryContext

    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        projectId: String? = null
    ): CustomResult<SyncResult<ProjectChannel>, Exception>

    suspend fun syncToServer(
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun forceSyncAll(
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun resolveConflicts(
        conflictedChannelIds: List<String>
    ): CustomResult<Int, Exception>

    // === Firebase Functions (서버 작업) ===

    suspend fun createChannel(
        projectId: String,
        name: String,
        description: String?,
        categoryId: String?
    ): CustomResult<ProjectChannel, Exception>

    suspend fun updateChannel(
        channelId: String,
        name: String?,
        description: String?,
        categoryId: String?
    ): CustomResult<Unit, Exception>

    suspend fun deleteChannel(channelId: String): CustomResult<Unit, Exception>
}
