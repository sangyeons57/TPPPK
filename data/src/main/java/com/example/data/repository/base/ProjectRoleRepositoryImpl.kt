package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.RoleRemoteDataSource
import com.example.domain.model.base.Role
import com.example.domain.model.data.project.RolePermission
import com.example.domain.repository.base.ProjectRoleRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.ProjectRoleRepositoryFactoryContext
import javax.inject.Inject

/**
 * Remote ProjectRole Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class ProjectRoleRepositoryImpl @Inject constructor(
    private val roleRemoteDataSource: RoleRemoteDataSource,
    override val factoryContext: ProjectRoleRepositoryFactoryContext
) : ProjectRoleRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        projectId: String?
    ): CustomResult<SyncResult<Role>, Exception> {
        return roleRemoteDataSource.syncFromServer(lastSyncCursor, projectId)
    }

    override suspend fun syncToServer(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return roleRemoteDataSource.syncToServer(projectId)
    }

    override suspend fun forceSyncAll(
        projectId: String?
    ): CustomResult<Int, Exception> {
        return roleRemoteDataSource.forceSyncAll(projectId)
    }

    override suspend fun resolveConflicts(
        conflictedRoleIds: List<String>
    ): CustomResult<Int, Exception> {
        return roleRemoteDataSource.resolveConflicts(conflictedRoleIds)
    }

    // === Firebase Functions (서버 작업) ===
    
    override suspend fun getRolePermissions(
        projectId: String,
        roleId: String
    ): CustomResult<List<RolePermission>, Exception> {
        return roleRemoteDataSource.getRolePermissions(projectId, roleId)
    }
}
