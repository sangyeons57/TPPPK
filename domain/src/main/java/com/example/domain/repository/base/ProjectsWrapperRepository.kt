package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.repository.factory.context.ProjectsWrapperRepositoryFactoryContext

/**
 * Remote ProjectsWrapper Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
interface ProjectsWrapperRepository {
    val factoryContext: ProjectsWrapperRepositoryFactoryContext

    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        userId: String? = null
    ): CustomResult<SyncResult<ProjectsWrapper>, Exception>

    suspend fun syncToServer(
        userId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun forceSyncAll(
        userId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun resolveConflicts(
        conflictedWrapperIds: List<String>
    ): CustomResult<Int, Exception>
}
