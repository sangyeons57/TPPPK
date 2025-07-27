package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.PermissionRemoteDataSource
import com.example.domain.model.base.Permission
import com.example.domain.repository.base.PermissionRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.PermissionRepositoryFactoryContext
import javax.inject.Inject

/**
 * Remote Permission Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class PermissionRepositoryImpl @Inject constructor(
    private val permissionRemoteDataSource: PermissionRemoteDataSource,
    override val factoryContext: PermissionRepositoryFactoryContext
) : PermissionRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        projectId: String?
    ): CustomResult<SyncResult<Permission>, Exception> {
        return permissionRemoteDataSource.syncFromServer(lastSyncCursor, projectId)
    }

    override suspend fun syncToServer(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return permissionRemoteDataSource.syncToServer(projectId)
    }

    override suspend fun forceSyncAll(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return permissionRemoteDataSource.forceSyncAll(projectId)
    }

    override suspend fun resolveConflicts(
        conflictedPermissionIds: List<String>
    ): CustomResult<Int, Exception> {
        return permissionRemoteDataSource.resolveConflicts(conflictedPermissionIds)
    }
}
