package com.example.data.datasource.local.projectrole

import com.example.data.db.dao.RoleDao
import com.example.data.model.local.RoleEntity
import com.example.data.model.local.RolePermissionEntity
import com.example.domain.model.Role
import com.example.domain.model.RolePermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectRoleLocalDataSource 인터페이스의 구현체입니다.
 * Room 데이터베이스를 사용하여 프로젝트 역할 관련 로컬 CRUD 작업을 수행합니다.
 * 
 * @param roleDao 역할 관련 데이터베이스 액세스 객체
 */
@Singleton
class ProjectRoleLocalDataSourceImpl @Inject constructor(
    private val roleDao: RoleDao
) : ProjectRoleLocalDataSource {

    /**
     * 특정 프로젝트의 모든 역할 목록을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 역할 목록
     */
    override suspend fun getRoles(projectId: String): List<Role> {
        val rolesWithPermissions = roleDao.getRolesWithPermissionsByProject(projectId)
        return rolesWithPermissions.map { (roleEntity, permissionEntities) ->
            roleEntity.toDomain(RolePermissionEntity.toPermissionMap(permissionEntities))
        }
    }

    /**
     * 특정 프로젝트의 역할 목록 실시간 스트림을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 역할 목록의 Flow
     */
    override fun getRolesStream(projectId: String): Flow<List<Role>> {
        return roleDao.observeRolesByProject(projectId).map { roleEntities ->
            roleEntities.map { roleEntity ->
                val permissions = roleDao.getPermissionsByRole(roleEntity.id)
                roleEntity.toDomain(RolePermissionEntity.toPermissionMap(permissions))
            }
        }
    }

    /**
     * 특정 역할의 상세 정보를 가져옵니다.
     * 
     * @param roleId 역할 ID
     * @return 역할 이름과 권한 맵 Pair 또는 null
     */
    override suspend fun getRoleDetails(roleId: String): Pair<String, Map<RolePermission, Boolean>>? {
        val roleWithPermissions = roleDao.getRoleWithPermissions(roleId) ?: return null
        val (roleEntity, permissionEntities) = roleWithPermissions
        return roleEntity.name to RolePermissionEntity.toPermissionMap(permissionEntities)
    }

    /**
     * 새 역할을 저장합니다.
     * 
     * @param role 역할 객체
     */
    override suspend fun saveRole(role: Role) {
        // Role ID가 null이 아닌지 확인
        val roleEntity = RoleEntity.fromDomain(role)
        val permissionEntities = RolePermissionEntity.fromPermissionMap(roleEntity.id, role.permissions)
        roleDao.insertRoleWithPermissions(roleEntity, permissionEntities)
    }

    /**
     * 여러 역할을 저장합니다.
     * 
     * @param roles 역할 목록
     */
    override suspend fun saveRoles(roles: List<Role>) {
        roles.forEach { role ->
            saveRole(role)
        }
    }

    /**
     * 역할을 업데이트합니다.
     * 
     * @param role 업데이트할 역할 객체
     * @return 업데이트 성공 여부
     */
    override suspend fun updateRole(role: Role): Boolean {
        if (role.id == null) return false
        
        val roleEntity = RoleEntity.fromDomain(role)
        val permissionEntities = RolePermissionEntity.fromPermissionMap(roleEntity.id, role.permissions)
        
        return try {
            roleDao.updateRoleWithPermissions(roleEntity, permissionEntities)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 역할을 삭제합니다.
     * 
     * @param roleId 삭제할 역할 ID
     * @return 삭제 성공 여부
     */
    override suspend fun deleteRole(roleId: String): Boolean {
        return try {
            roleDao.deleteRoleWithPermissions(roleId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 프로젝트의 모든 역할을 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     */
    override suspend fun deleteProjectRoles(projectId: String) {
        val roles = roleDao.getRolesByProject(projectId)
        roles.forEach { role ->
            roleDao.deleteRoleWithPermissions(role.id)
        }
    }
} 