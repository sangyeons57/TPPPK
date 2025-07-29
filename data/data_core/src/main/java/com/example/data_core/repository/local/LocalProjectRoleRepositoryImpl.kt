package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.RolesDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.Role
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.domain.repository.local.LocalProjectRoleRepository
import com.example.mapper.RoleEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
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
    private val rolesDao: RolesDao,
    private val outboxDao: OutboxDao,
    private val mapper: RoleEntityMapper
) : LocalProjectRoleRepository {

    companion object {
        private const val TAG = "LocalProjectRoleRepository"
        private const val COLLECTION_NAME = "roles"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeRoleById(roleId: String): Flow<Role?> {
        Log.d(TAG, "observeRoleById: $roleId")
        return rolesDao.observeRoleById(roleId).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeRolesByProject(projectId: String): Flow<List<Role>> {
        Log.d(TAG, "observeRolesByProject: $projectId")
        return rolesDao.observeRolesByProject(projectId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeByName(name: Name): Flow<Role?> {
        Log.d(TAG, "observeByName: ${name.value}")
        return rolesDao.observeByName(name.value).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeAllByName(name: String, limit: Int): Flow<List<Role>> {
        Log.d(TAG, "observeAllByName: name='$name', limit=$limit")
        return rolesDao.observeAllByName(name, limit).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeRoles(roleIds: List<String>): Flow<List<Role>> {
        Log.d(TAG, "observeRoles: ${roleIds.size} roles")
        return rolesDao.observeRoles(roleIds).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeRoleUpdatedAt(roleId: String): Flow<Long?> {
        Log.d(TAG, "observeRoleUpdatedAt: $roleId")
        return rolesDao.observeRoleById(roleId).map { it?.updatedAt?.toEpochMilli() }
    }

    override fun observeAllRoles(): Flow<List<Role>> {
        Log.d(TAG, "observeAllRoles")
        return rolesDao.observeAllRoles().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeDefaultRoles(): Flow<List<Role>> {
        Log.d(TAG, "observeDefaultRoles")
        return rolesDao.observeDefaultRoles().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeSystemRoles(): Flow<List<Role>> {
        Log.d(TAG, "observeSystemRoles")
        return rolesDao.observeSystemRoles().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    // === 단순 읽기 작업 ===

    override suspend fun getRoleById(roleId: String): Role? {
        Log.d(TAG, "getRoleById: $roleId")
        return try {
            rolesDao.getRoleById(roleId)?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getRoleById failed", e)
            null
        }
    }

    override suspend fun getRolesByProject(projectId: String): List<Role> {
        Log.d(TAG, "getRolesByProject: $projectId")
        return try {
            rolesDao.getRolesByProject(projectId).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getRolesByProject failed", e)
            emptyList()
        }
    }

    override suspend fun getRoleByName(name: Name): Role? {
        Log.d(TAG, "getRoleByName: ${name.value}")
        return try {
            rolesDao.getRoleByName(name.value)?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getRoleByName failed", e)
            null
        }
    }

    override suspend fun searchRolesByName(name: String, limit: Int): List<Role> {
        Log.d(TAG, "searchRolesByName: name='$name', limit=$limit")
        return try {
            rolesDao.searchRolesByName(name, limit).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "searchRolesByName failed", e)
            emptyList()
        }
    }

    override suspend fun getRolesByIds(roleIds: List<String>): List<Role> {
        Log.d(TAG, "getRolesByIds: ${roleIds.size} roles")
        return try {
            rolesDao.getRolesByIds(roleIds).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getRolesByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllRoles(limit: Int?): List<Role> {
        Log.d(TAG, "getAllRoles: limit=$limit")
        return try {
            rolesDao.getAllRoles().map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getAllRoles failed", e)
            emptyList()
        }
    }

    override suspend fun getDefaultRoles(): List<Role> {
        Log.d(TAG, "getDefaultRoles")
        return try {
            rolesDao.getDefaultRoles().map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getDefaultRoles failed", e)
            emptyList()
        }
    }

    override suspend fun getSystemRoles(): List<Role> {
        Log.d(TAG, "getSystemRoles")
        return try {
            rolesDao.getSystemRoles().map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getSystemRoles failed", e)
            emptyList()
        }
    }

    override suspend fun getOwnerRole(): Role? {
        Log.d(TAG, "getOwnerRole")
        return try {
            rolesDao.getOwnerRole()?.let { mapper.toDomain(it) }
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
            rolesDao.insertRole(mapper.toEntity(role))

            // 2. Outbox에 동기화 작업 추가
            val operation = if (role.isNew) "CREATE" else "UPDATE"
            addToOutbox(
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
            rolesDao.insertRoles(roles.map { mapper.toEntity(it) })

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
            if (rolesDao.isSystemRole(roleId)) {
                return CustomResult.Failure(IllegalArgumentException("Cannot delete system role: $roleId"))
            }

            // 2. Room DB에서 삭제 (실제로는 soft delete)
            rolesDao.deleteRole(roleId)

            // 3. Outbox에 삭제 작업 추가
            addToOutbox(
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
            val currentRole = rolesDao.getRoleById(roleId)?.let { mapper.toDomain(it) }
                ?: return CustomResult.Failure(IllegalArgumentException("Role not found: $roleId"))

            // 2. 시스템 역할인지 확인
            if (rolesDao.isSystemRole(roleId)) {
                return CustomResult.Failure(IllegalArgumentException("Cannot update system role name: $roleId"))
            }

            // 3. 이름 변경
            val updatedRole = currentRole.changeName(name)

            // 4. 저장 (Outbox 포함)
            saveRole(updatedRole)

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
            val currentRole = rolesDao.getRoleById(roleId)?.let { mapper.toDomain(it) }
                ?: return CustomResult.Failure(IllegalArgumentException("Role not found: $roleId"))

            // 2. 시스템 역할인지 확인
            if (rolesDao.isSystemRole(roleId)) {
                return CustomResult.Failure(IllegalArgumentException("Cannot update system role default status: $roleId"))
            }

            // 3. 기본 상태 변경
            val updatedRole = currentRole.setDefault(isDefault)

            // 4. 저장 (Outbox 포함)
            saveRole(updatedRole)

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
            val existingOwner = rolesDao.getOwnerRole()?.let { mapper.toDomain(it) }
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
            rolesDao.roleExists(roleId)
        } catch (e: Exception) {
            Log.e(TAG, "roleExists failed", e)
            false
        }
    }

    override suspend fun nameExists(name: Name, excludeRoleId: String?): Boolean {
        return try {
            rolesDao.nameExists(name.value, excludeRoleId)
        } catch (e: Exception) {
            Log.e(TAG, "nameExists failed", e)
            false
        }
    }

    override suspend fun isSystemRole(roleId: String): Boolean {
        return try {
            rolesDao.isSystemRole(roleId)
        } catch (e: Exception) {
            Log.e(TAG, "isSystemRole failed", e)
            false
        }
    }

    override suspend fun isDefaultRole(roleId: String): Boolean {
        return try {
            rolesDao.isDefaultRole(roleId)
        } catch (e: Exception) {
            Log.e(TAG, "isDefaultRole failed", e)
            false
        }
    }

    override suspend fun getTotalRoleCount(): Int {
        return try {
            rolesDao.getTotalRoleCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalRoleCount failed", e)
            0
        }
    }

    override suspend fun getRoleCountByProject(projectId: String): Int {
        return try {
            rolesDao.getRoleCountByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getRoleCountByProject failed", e)
            0
        }
    }

    override suspend fun getDefaultRoleCount(): Int {
        return try {
            rolesDao.getDefaultRoleCount()
        } catch (e: Exception) {
            Log.e(TAG, "getDefaultRoleCount failed", e)
            0
        }
    }

    override suspend fun getSystemRoleCount(): Int {
        return try {
            rolesDao.getSystemRoleCount()
        } catch (e: Exception) {
            Log.e(TAG, "getSystemRoleCount failed", e)
            0
        }
    }

    override suspend fun clearAllRoles(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllRoles")

            rolesDao.deleteAllRoles()

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
            rolesDao.getRolesUpdatedAfter(timestamp).map { mapper.toDomain(it) }
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

            val outboxEntity = OutboxEntity(
                id = UUID.randomUUID().toString(),
                collectionName = COLLECTION_NAME,
                documentId = roleId,
                operation = operation,
                payload = payload,
                localTimestamp = System.currentTimeMillis(),
                retries = 0
            )
            outboxDao.insertOutbox(outboxEntity)

            Log.d(TAG, "Added to outbox: $roleId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}