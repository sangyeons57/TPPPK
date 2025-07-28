package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalPermissionsDataSource
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission
import com.example.domain.repository.local.LocalPermissionRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Permission Repository Implementation (SSOT)
 * Room Database 전용 구현체 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 */
@Singleton
class LocalPermissionRepositoryImpl @Inject constructor(
    private val localPermissionsDataSource: LocalPermissionsDataSource
) : LocalPermissionRepository {

    companion object {
        private const val TAG = "LocalPermissionRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observePermissionById(permissionId: String): Flow<Permission?> {
        Log.d(TAG, "observePermissionById: $permissionId")
        return localPermissionsDataSource.observePermissionById(permissionId)
    }

    override fun observePermissionsByRole(roleId: String): Flow<List<Permission>> {
        Log.d(TAG, "observePermissionsByRole: $roleId")
        return localPermissionsDataSource.observePermissionsByRole(roleId)
    }

    override fun observePermissions(permissionIds: List<String>): Flow<List<Permission>> {
        Log.d(TAG, "observePermissions: ${permissionIds.size} permissions")
        return localPermissionsDataSource.observePermissions(permissionIds)
    }

    override fun observePermissionUpdatedAt(permissionId: String): Flow<Long?> {
        Log.d(TAG, "observePermissionUpdatedAt: $permissionId")
        return localPermissionsDataSource.observePermissionUpdatedAt(permissionId)
    }

    override fun observeAllPermissions(): Flow<List<Permission>> {
        Log.d(TAG, "observeAllPermissions")
        return localPermissionsDataSource.observeAllPermissions()
    }

    override fun observePermissionsByType(rolePermission: RolePermission): Flow<List<Permission>> {
        Log.d(TAG, "observePermissionsByType: $rolePermission")
        return localPermissionsDataSource.observePermissionsByType(rolePermission)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getPermissionById(permissionId: String): Permission? {
        Log.d(TAG, "getPermissionById: $permissionId")
        return try {
            localPermissionsDataSource.getPermissionById(permissionId)
        } catch (e: Exception) {
            Log.e(TAG, "getPermissionById failed", e)
            null
        }
    }

    override suspend fun getPermissionsByRole(roleId: String): List<Permission> {
        Log.d(TAG, "getPermissionsByRole: $roleId")
        return try {
            localPermissionsDataSource.getPermissionsByRole(roleId)
        } catch (e: Exception) {
            Log.e(TAG, "getPermissionsByRole failed", e)
            emptyList()
        }
    }

    override suspend fun getPermissionsByIds(permissionIds: List<String>): List<Permission> {
        Log.d(TAG, "getPermissionsByIds: ${permissionIds.size} permissions")
        return try {
            localPermissionsDataSource.getPermissionsByIds(permissionIds)
        } catch (e: Exception) {
            Log.e(TAG, "getPermissionsByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllPermissions(limit: Int?): List<Permission> {
        Log.d(TAG, "getAllPermissions: limit=$limit")
        return try {
            localPermissionsDataSource.getAllPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "getAllPermissions failed", e)
            emptyList()
        }
    }

    override suspend fun getPermissionsByType(rolePermission: RolePermission): List<Permission> {
        Log.d(TAG, "getPermissionsByType: $rolePermission")
        return try {
            localPermissionsDataSource.getPermissionsByType(rolePermission)
        } catch (e: Exception) {
            Log.e(TAG, "getPermissionsByType failed", e)
            emptyList()
        }
    }

    override suspend fun getPermissionsByRoles(roleIds: List<String>): List<Permission> {
        Log.d(TAG, "getPermissionsByRoles: ${roleIds.size} roles")
        return try {
            localPermissionsDataSource.getPermissionsByRoles(roleIds)
        } catch (e: Exception) {
            Log.e(TAG, "getPermissionsByRoles failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun savePermission(permission: Permission): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "savePermission: ${permission.id}")

            // 1. Room DB에 저장
            localPermissionsDataSource.savePermission(permission)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (permission.isNew) "CREATE" else "UPDATE"
            localPermissionsDataSource.addToOutbox(
                permissionId = permission.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Permission saved and added to outbox: ${permission.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "savePermission failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun savePermissions(permissions: List<Permission>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "savePermissions: ${permissions.size} permissions")

            if (permissions.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localPermissionsDataSource.savePermissions(permissions)

            Log.d(TAG, "Bulk permissions saved: ${permissions.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "savePermissions failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deletePermission(permissionId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deletePermission: $permissionId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localPermissionsDataSource.deletePermission(permissionId)

            // 2. Outbox에 삭제 작업 추가
            localPermissionsDataSource.addToOutbox(
                permissionId = permissionId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Permission deleted and added to outbox: $permissionId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deletePermission failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deletePermissionsByRole(roleId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deletePermissionsByRole: $roleId")

            // 1. 해당 역할의 모든 권한 조회
            val permissions = localPermissionsDataSource.getPermissionsByRole(roleId)

            // 2. 각 권한 삭제
            permissions.forEach { permission ->
                deletePermission(permission.id.value)
            }

            Log.d(TAG, "Permissions deleted by role: $roleId, count: ${permissions.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deletePermissionsByRole failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun createPermissionByType(rolePermission: RolePermission): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "createPermissionByType: $rolePermission")

            // 1. 권한 생성
            val permission = Permission.create(rolePermission)

            // 2. 저장 (Outbox 포함)
            savePermission(permission)

            Log.d(TAG, "Permission created by type: $rolePermission")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "createPermissionByType failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun permissionExists(permissionId: String): Boolean {
        return try {
            localPermissionsDataSource.permissionExists(permissionId)
        } catch (e: Exception) {
            Log.e(TAG, "permissionExists failed", e)
            false
        }
    }

    override suspend fun roleHasPermission(
        roleId: String,
        rolePermission: RolePermission
    ): Boolean {
        return try {
            localPermissionsDataSource.roleHasPermission(roleId, rolePermission)
        } catch (e: Exception) {
            Log.e(TAG, "roleHasPermission failed", e)
            false
        }
    }

    override suspend fun permissionTypeExists(rolePermission: RolePermission): Boolean {
        return try {
            localPermissionsDataSource.permissionTypeExists(rolePermission)
        } catch (e: Exception) {
            Log.e(TAG, "permissionTypeExists failed", e)
            false
        }
    }

    override suspend fun getTotalPermissionCount(): Int {
        return try {
            localPermissionsDataSource.getTotalPermissionCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalPermissionCount failed", e)
            0
        }
    }

    override suspend fun getPermissionCountByRole(roleId: String): Int {
        return try {
            localPermissionsDataSource.getPermissionCountByRole(roleId)
        } catch (e: Exception) {
            Log.e(TAG, "getPermissionCountByRole failed", e)
            0
        }
    }

    override suspend fun getPermissionCountByType(rolePermission: RolePermission): Int {
        return try {
            localPermissionsDataSource.getPermissionCountByType(rolePermission)
        } catch (e: Exception) {
            Log.e(TAG, "getPermissionCountByType failed", e)
            0
        }
    }

    override suspend fun clearAllPermissions(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllPermissions")

            localPermissionsDataSource.clearAllPermissions()

            Log.d(TAG, "All permissions cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllPermissions failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getPermissionsUpdatedAfter(timestamp: Instant): List<Permission> {
        return try {
            localPermissionsDataSource.getPermissionsUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getPermissionsUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        permissionId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: permissionId=$permissionId, operation=$operation")

            localPermissionsDataSource.addToOutbox(permissionId, operation, payload)

            Log.d(TAG, "Added to outbox: $permissionId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}