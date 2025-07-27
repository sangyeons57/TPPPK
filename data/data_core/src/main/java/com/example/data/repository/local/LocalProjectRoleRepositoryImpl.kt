package com.example.data.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.local.LocalRolesDataSource
import com.example.domain.model.base.Role
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.domain.repository.local.LocalProjectRoleRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Project Role Repository Implementation (SSOT)
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
class LocalProjectRoleRepositoryImpl @Inject constructor(
    private val localRolesDataSource: LocalRolesDataSource
) : LocalProjectRoleRepository {

    companion object {
        private const val TAG = "LocalProjectRoleRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeRoleById(roleId: String): Flow<Role?> {
        Log.d(TAG, "observeRoleById: $roleId")
        return localRolesDataSource.observeRoleById(roleId)
    }

    override fun observeRolesByProject(projectId: String): Flow<List<Role>> {
        Log.d(TAG, "observeRolesByProject: $projectId")
        return localRolesDataSource.observeRolesByProject(projectId)
    }

    override fun observeByName(name: Name): Flow<Role?> {
        Log.d(TAG, "observeByName: ${name.value}")
        return localRolesDataSource.observeByName(name)
    }

    override fun observeAllByName(name: String, limit: Int): Flow<List<Role>> {
        Log.d(TAG, "observeAllByName: name='$name', limit=$limit")
        return localRolesDataSource.observeAllByName(name, limit)
    }

    override fun observeRoles(roleIds: List<String>): Flow<List<Role>> {
        Log.d(TAG, "observeRoles: ${roleIds.size} roles")
        return localRolesDataSource.observeRoles(roleIds)
    }

    override fun observeRoleUpdatedAt(roleId: String): Flow<Long?> {
        Log.d(TAG, "observeRoleUpdatedAt: $roleId")
        return localRolesDataSource.observeRoleUpdatedAt(roleId)
    }

    override fun observeAllRoles(): Flow<List<Role>> {
        Log.d(TAG, "observeAllRoles")
        return localRolesDataSource.observeAllRoles()
    }

    override fun observeDefaultRoles(): Flow<List<Role>> {
        Log.d(TAG, "observeDefaultRoles")
        return localRolesDataSource.observeDefaultRoles()
    }

    override fun observeSystemRoles(): Flow<List<Role>> {
        Log.d(TAG, "observeSystemRoles")
        return localRolesDataSource.observeSystemRoles()
    }

    // === 단순 읽기 작업 ===

    override suspend fun getRoleById(roleId: String): Role? {
        Log.d(TAG, "getRoleById: $roleId")
        return try {
            localRolesDataSource.getRoleById(roleId)
        } catch (e: Exception) {
            Log.e(TAG, "getRoleById failed", e)
            null
        }
    }

    override suspend fun getRolesByProject(projectId: String): List<Role> {
        Log.d(TAG, "getRolesByProject: $projectId")
        return try {
            localRolesDataSource.getRolesByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getRolesByProject failed", e)
            emptyList()
        }
    }

    override suspend fun getRoleByName(name: Name): Role? {
        Log.d(TAG, "getRoleByName: ${name.value}")
        return try {
            localRolesDataSource.getRoleByName(name)
        } catch (e: Exception) {
            Log.e(TAG, "getRoleByName failed", e)
            null
        }
    }

    override suspend fun searchRolesByName(name: String, limit: Int): List<Role> {
        Log.d(TAG, "searchRolesByName: name='$name', limit=$limit")
        return try {
            localRolesDataSource.searchRolesByName(name, limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchRolesByName failed", e)
            emptyList()
        }
    }

    override suspend fun getRolesByIds(roleIds: List<String>): List<Role> {
        Log.d(TAG, "getRolesByIds: ${roleIds.size} roles")
        return try {
            localRolesDataSource.getRolesByIds(roleIds)
        } catch (e: Exception) {
            Log.e(TAG, "getRolesByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllRoles(limit: Int?): List<Role> {
        Log.d(TAG, "getAllRoles: limit=$limit")
        return try {
            localRolesDataSource.getAllRoles(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllRoles failed", e)
            emptyList()
        }
    }

    override suspend fun getDefaultRoles(): List<Role> {
        Log.d(TAG, "getDefaultRoles")
        return try {
            localRolesDataSource.getDefaultRoles()
        } catch (e: Exception) {
            Log.e(TAG, "getDefaultRoles failed", e)
            emptyList()
        }
    }

    override suspend fun getSystemRoles(): List<Role> {
        Log.d(TAG, "getSystemRoles")
        return try {
            localRolesDataSource.getSystemRoles()
        } catch (e: Exception) {
            Log.e(TAG, "getSystemRoles failed", e)
            emptyList()
        }
    }

    override suspend fun getOwnerRole(): Role? {
        Log.d(TAG, "getOwnerRole")
        return try {
            localRolesDataSource.getOwnerRole()
        } catch (e: Exception) {
            Log.e(TAG, "getOwnerRole failed", e)
            null
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveRole(role: Role): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveRole: ${role.id}")

            // 1. Room DB에 저장
            localRolesDataSource.saveRole(role)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (role.isNew) "CREATE" else "UPDATE"
            localRolesDataSource.addToOutbox(
                roleId = role.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Role saved and added to outbox: ${role.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveRole failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveRoles(roles: List<Role>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveRoles: ${roles.size} roles")

            if (roles.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localRolesDataSource.saveRoles(roles)

            Log.d(TAG, "Bulk roles saved: ${roles.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveRoles failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteRole(roleId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteRole: $roleId")

            // 1. 시스템 역할인지 확인
            if (localRolesDataSource.isSystemRole(roleId)) {
                return CustomResult.Failure(IllegalArgumentException("Cannot delete system role: $roleId"))
            }

            // 2. Room DB에서 삭제 (실제로는 soft delete)
            localRolesDataSource.deleteRole(roleId)

            // 3. Outbox에 삭제 작업 추가
            localRolesDataSource.addToOutbox(
                roleId = roleId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Role deleted and added to outbox: $roleId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteRole failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateRoleName(
        roleId: String,
        name: Name
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "updateRoleName: roleId=$roleId, name=${name.value}")

            // 1. 현재 역할 조회
            val currentRole = localRolesDataSource.getRoleById(roleId)
                ?: return CustomResult.Failure(IllegalArgumentException("Role not found: $roleId"))

            // 2. 시스템 역할인지 확인
            if (localRolesDataSource.isSystemRole(roleId)) {
                return CustomResult.Failure(IllegalArgumentException("Cannot update system role name: $roleId"))
            }

            // 3. 이름 변경
            currentRole.changeName(name)

            // 4. 저장 (Outbox 포함)
            saveRole(currentRole)

            Log.d(TAG, "Role name updated: $roleId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateRoleName failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateRoleDefaultStatus(
        roleId: String,
        isDefault: RoleIsDefault
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "updateRoleDefaultStatus: roleId=$roleId, isDefault=$isDefault")

            // 1. 현재 역할 조회
            val currentRole = localRolesDataSource.getRoleById(roleId)
                ?: return CustomResult.Failure(IllegalArgumentException("Role not found: $roleId"))

            // 2. 시스템 역할인지 확인
            if (localRolesDataSource.isSystemRole(roleId)) {
                return CustomResult.Failure(IllegalArgumentException("Cannot update system role default status: $roleId"))
            }

            // 3. 기본 상태 변경
            currentRole.setDefault(isDefault)

            // 4. 저장 (Outbox 포함)
            saveRole(currentRole)

            Log.d(TAG, "Role default status updated: $roleId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateRoleDefaultStatus failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun createOwnerRole(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "createOwnerRole")

            // 1. OWNER 역할이 이미 존재하는지 확인
            val existingOwner = localRolesDataSource.getOwnerRole()
            if (existingOwner != null) {
                Log.d(TAG, "OWNER role already exists")
                return CustomResult.Success(Unit)
            }

            // 2. OWNER 역할 생성
            val ownerRole = Role.createOwner()

            // 3. 저장 (Outbox 포함)
            saveRole(ownerRole)

            Log.d(TAG, "OWNER role created")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "createOwnerRole failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun roleExists(roleId: String): Boolean {
        return try {
            localRolesDataSource.roleExists(roleId)
        } catch (e: Exception) {
            Log.e(TAG, "roleExists failed", e)
            false
        }
    }

    override suspend fun nameExists(name: Name, excludeRoleId: String?): Boolean {
        return try {
            localRolesDataSource.nameExists(name, excludeRoleId)
        } catch (e: Exception) {
            Log.e(TAG, "nameExists failed", e)
            false
        }
    }

    override suspend fun isSystemRole(roleId: String): Boolean {
        return try {
            localRolesDataSource.isSystemRole(roleId)
        } catch (e: Exception) {
            Log.e(TAG, "isSystemRole failed", e)
            false
        }
    }

    override suspend fun isDefaultRole(roleId: String): Boolean {
        return try {
            localRolesDataSource.isDefaultRole(roleId)
        } catch (e: Exception) {
            Log.e(TAG, "isDefaultRole failed", e)
            false
        }
    }

    override suspend fun getTotalRoleCount(): Int {
        return try {
            localRolesDataSource.getTotalRoleCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalRoleCount failed", e)
            0
        }
    }

    override suspend fun getRoleCountByProject(projectId: String): Int {
        return try {
            localRolesDataSource.getRoleCountByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getRoleCountByProject failed", e)
            0
        }
    }

    override suspend fun getDefaultRoleCount(): Int {
        return try {
            localRolesDataSource.getDefaultRoleCount()
        } catch (e: Exception) {
            Log.e(TAG, "getDefaultRoleCount failed", e)
            0
        }
    }

    override suspend fun getSystemRoleCount(): Int {
        return try {
            localRolesDataSource.getSystemRoleCount()
        } catch (e: Exception) {
            Log.e(TAG, "getSystemRoleCount failed", e)
            0
        }
    }

    override suspend fun clearAllRoles(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllRoles")

            localRolesDataSource.clearAllRoles()

            Log.d(TAG, "All roles cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllRoles failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getRolesUpdatedAfter(timestamp: Instant): List<Role> {
        return try {
            localRolesDataSource.getRolesUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getRolesUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        roleId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: roleId=$roleId, operation=$operation")

            localRolesDataSource.addToOutbox(roleId, operation, payload)

            Log.d(TAG, "Added to outbox: $roleId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}