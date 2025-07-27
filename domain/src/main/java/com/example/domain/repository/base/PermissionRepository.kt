package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Permission
import com.example.domain.repository.factory.context.PermissionRepositoryFactoryContext

/**
 * Remote Permission Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
interface PermissionRepository {
    val factoryContext: PermissionRepositoryFactoryContext

    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        projectId: String? = null
    ): CustomResult<SyncResult<Permission>, Exception>

    suspend fun syncToServer(
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun forceSyncAll(
        projectId: String? = null
    ): CustomResult<Int, Exception>

    suspend fun resolveConflicts(
        conflictedPermissionIds: List<String>
    ): CustomResult<Int, Exception>
}
