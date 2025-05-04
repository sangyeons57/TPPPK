package com.example.data.datasource.local.projectrole

import com.example.domain.model.Role
import com.example.domain.model.RolePermission
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 역할 관련 로컬 데이터 소스 인터페이스
 * Room 데이터베이스를 사용하여 프로젝트 내 역할 및 권한 관련 로컬 CRUD 작업을 정의합니다.
 */
interface ProjectRoleLocalDataSource {
    /**
     * 특정 프로젝트의 모든 역할 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 역할 목록
     */
    suspend fun getRoles(projectId: String): List<Role>
    
    /**
     * 특정 프로젝트의 역할 목록 실시간 스트림을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 역할 목록의 Flow
     */
    fun getRolesStream(projectId: String): Flow<List<Role>>
    
    /**
     * 특정 역할의 상세 정보를 가져옵니다.
     * @param roleId 역할 ID
     * @return 역할 이름과 권한 맵 Pair 또는 null
     */
    suspend fun getRoleDetails(roleId: String): Pair<String, Map<RolePermission, Boolean>>?
    
    /**
     * 새 역할을 저장합니다.
     * @param role 역할 객체
     */
    suspend fun saveRole(role: Role)
    
    /**
     * 여러 역할을 저장합니다.
     * @param roles 역할 목록
     */
    suspend fun saveRoles(roles: List<Role>)
    
    /**
     * 역할을 업데이트합니다.
     * @param role 업데이트할 역할 객체
     * @return 업데이트 성공 여부
     */
    suspend fun updateRole(role: Role): Boolean
    
    /**
     * 역할을 삭제합니다.
     * @param roleId 삭제할 역할 ID
     * @return 삭제 성공 여부
     */
    suspend fun deleteRole(roleId: String): Boolean
    
    /**
     * 프로젝트의 모든 역할을 삭제합니다.
     * @param projectId 프로젝트 ID
     */
    suspend fun deleteProjectRoles(projectId: String)
}