package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.project.RolePermission
import com.example.domain.repository.factory.context.ProjectRoleRepositoryFactoryContext

/**
 * Remote ProjectRole Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
interface ProjectRoleRepository {
    val factoryContext: ProjectRoleRepositoryFactoryContext

    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        projectId: String? = null
    ): CustomResult<SyncResult<Role>, Exception>

    suspend fun syncToServer(
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun forceSyncAll(
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun resolveConflicts(
        conflictedRoleIds: List<String>
    ): CustomResult<Int, Exception>

    // === Firebase Functions (서버 작업) ===
    
    suspend fun getRolePermissions(projectId: String, roleId: String): CustomResult<List<RolePermission>, Exception>
}
