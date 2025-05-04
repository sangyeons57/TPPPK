package com.example.data.db.dao

import androidx.room.*
import com.example.data.model.local.RoleEntity
import com.example.data.model.local.RolePermissionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 역할 관련 데이터베이스 액세스 인터페이스
 */
@Dao
interface RoleDao {
    // --- Role 관련 쿼리 ---
    
    /**
     * 특정 프로젝트의 모든 역할을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 역할 목록
     */
    @Query("SELECT * FROM roles WHERE projectId = :projectId ORDER BY name")
    suspend fun getRolesByProject(projectId: String): List<RoleEntity>
    
    /**
     * 특정 프로젝트의 모든 역할을 Flow로 관찰합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 역할 목록 Flow
     */
    @Query("SELECT * FROM roles WHERE projectId = :projectId ORDER BY name")
    fun observeRolesByProject(projectId: String): Flow<List<RoleEntity>>
    
    /**
     * 특정 역할 정보를 가져옵니다.
     * 
     * @param roleId 역할 ID
     * @return 역할 엔티티
     */
    @Query("SELECT * FROM roles WHERE id = :roleId")
    suspend fun getRoleById(roleId: String): RoleEntity?
    
    /**
     * 새 역할을 삽입합니다.
     * 
     * @param role 삽입할 역할 엔티티
     * @return 삽입된 행의 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRole(role: RoleEntity): Long
    
    /**
     * 여러 역할을 삽입합니다.
     * 
     * @param roles 삽입할 역할 엔티티 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoles(roles: List<RoleEntity>)
    
    /**
     * 역할 정보를 업데이트합니다.
     * 
     * @param role 업데이트할 역할 엔티티
     * @return 영향받은 행 수
     */
    @Update
    suspend fun updateRole(role: RoleEntity): Int
    
    /**
     * 역할을 삭제합니다.
     * 
     * @param roleId 삭제할 역할 ID
     * @return 영향받은 행 수
     */
    @Query("DELETE FROM roles WHERE id = :roleId")
    suspend fun deleteRole(roleId: String): Int
    
    /**
     * 특정 프로젝트의 모든 역할을 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 영향받은 행 수
     */
    @Query("DELETE FROM roles WHERE projectId = :projectId")
    suspend fun deleteRolesByProject(projectId: String): Int
    
    // --- RolePermission 관련 쿼리 ---
    
    /**
     * 특정 역할의 모든 권한을 가져옵니다.
     * 
     * @param roleId 역할 ID
     * @return 권한 엔티티 목록
     */
    @Query("SELECT * FROM role_permissions WHERE roleId = :roleId")
    suspend fun getPermissionsByRole(roleId: String): List<RolePermissionEntity>
    
    /**
     * 특정 역할의 모든 권한을 Flow로 관찰합니다.
     * 
     * @param roleId 역할 ID
     * @return 권한 엔티티 목록 Flow
     */
    @Query("SELECT * FROM role_permissions WHERE roleId = :roleId")
    fun observePermissionsByRole(roleId: String): Flow<List<RolePermissionEntity>>
    
    /**
     * 새 권한을 삽입합니다.
     * 
     * @param permission 삽입할 권한 엔티티
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermission(permission: RolePermissionEntity)
    
    /**
     * 여러 권한을 삽입합니다.
     * 
     * @param permissions 삽입할 권한 엔티티 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissions(permissions: List<RolePermissionEntity>)
    
    /**
     * 특정 역할의 모든 권한을 삭제합니다.
     * 
     * @param roleId 역할 ID
     * @return 영향받은 행 수
     */
    @Query("DELETE FROM role_permissions WHERE roleId = :roleId")
    suspend fun deletePermissionsByRole(roleId: String): Int
    
    /**
     * 특정 역할과 권한 조합을 삭제합니다.
     * 
     * @param roleId 역할 ID
     * @param permission 권한 문자열
     * @return 영향받은 행 수
     */
    @Query("DELETE FROM role_permissions WHERE roleId = :roleId AND permission = :permission")
    suspend fun deleteRolePermission(roleId: String, permission: String): Int
    
    // --- 트랜잭션 ---
    
    /**
     * 역할과 그에 연관된 모든 권한을 가져옵니다.
     * 
     * @param roleId 역할 ID
     * @return 역할 엔티티와 권한 엔티티 목록의 쌍, 역할이 없으면 null
     */
    @Transaction
    suspend fun getRoleWithPermissions(roleId: String): Pair<RoleEntity, List<RolePermissionEntity>>? {
        val role = getRoleById(roleId) ?: return null
        val permissions = getPermissionsByRole(roleId)
        return role to permissions
    }
    
    /**
     * 특정 프로젝트의 모든 역할과 각 역할의 권한을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 역할 엔티티와 권한 엔티티 목록의 쌍 목록
     */
    @Transaction
    suspend fun getRolesWithPermissionsByProject(projectId: String): List<Pair<RoleEntity, List<RolePermissionEntity>>> {
        val roles = getRolesByProject(projectId)
        return roles.map { role ->
            val permissions = getPermissionsByRole(role.id)
            role to permissions
        }
    }
    
    /**
     * 역할과 그 권한들을 동시에 저장합니다.
     * 
     * @param role 저장할 역할 엔티티
     * @param permissions 저장할 권한 엔티티 목록
     */
    @Transaction
    suspend fun insertRoleWithPermissions(role: RoleEntity, permissions: List<RolePermissionEntity>) {
        insertRole(role)
        insertPermissions(permissions)
    }
    
    /**
     * 역할과 그 권한들을 동시에 업데이트합니다.
     * 
     * @param role 업데이트할 역할 엔티티
     * @param permissions 업데이트할 권한 엔티티 목록
     */
    @Transaction
    suspend fun updateRoleWithPermissions(role: RoleEntity, permissions: List<RolePermissionEntity>) {
        updateRole(role)
        deletePermissionsByRole(role.id)
        insertPermissions(permissions)
    }
    
    /**
     * 역할과 그에 연관된 모든 권한을 삭제합니다.
     * 
     * @param roleId 삭제할 역할 ID
     */
    @Transaction
    suspend fun deleteRoleWithPermissions(roleId: String) {
        deletePermissionsByRole(roleId) // 외래 키 제약 조건으로 인해 먼저 권한 삭제
        deleteRole(roleId)
    }
} 