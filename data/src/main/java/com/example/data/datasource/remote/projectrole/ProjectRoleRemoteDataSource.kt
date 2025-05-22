package com.example.data.datasource.remote.projectrole

import com.example.domain.model.Role
import com.example.domain.model.RolePermission
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 역할 관련 원격 데이터 소스 인터페이스
 * 프로젝트 내 역할 및 권한 관련 Firebase Firestore 작업을 정의합니다.
 */
interface ProjectRoleRemoteDataSource {
    /**
     * 특정 프로젝트의 모든 역할 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 역할 목록
     */
    suspend fun getRoles(projectId: String): Result<List<Role>>
    
    /**
     * 특정 프로젝트의 역할 목록 실시간 스트림을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 역할 목록의 Flow
     */
    fun getRolesStream(projectId: String): Flow<List<Role>>
    
    /**
     * 특정 프로젝트의 역할 목록을 Firestore에서 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 작업 성공 여부
     */
    suspend fun fetchRoles(projectId: String): Result<Unit>
    
    /**
     * 특정 역할의 상세 정보를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param roleId 역할 ID (실제 문서 ID)
     * @return 역할 정보 또는 null (에러 발생 시 Result.failure)
     */
    suspend fun getRoleDetails(projectId: String, roleId: String): Result<Role?>
    
    /**
     * 새 역할을 생성합니다.
     * @param projectId 프로젝트 ID
     * @param name 역할 이름
     * @param permissions 권한 리스트
     * @param isDefault 기본 역할 여부
     * @return 새로 생성된 역할 ID
     */
    suspend fun createRole(
        projectId: String,
        name: String,
        permissions: List<RolePermission>,
        isDefault: Boolean
    ): Result<String>
    
    /**
     * 역할을 업데이트합니다.
     * @param projectId 프로젝트 ID
     * @param roleId 역할 ID
     * @param name 새 역할 이름 (null이면 변경하지 않음)
     * @param permissions 새 권한 리스트 (null이면 변경하지 않음)
     * @param isDefault 기본 역할 여부 (null이면 변경하지 않음)
     * @return 작업 성공 여부
     */
    suspend fun updateRole(
        projectId: String,
        roleId: String,
        name: String?,
        permissions: List<RolePermission>?,
        isDefault: Boolean?
    ): Result<Unit>
    
    /**
     * 역할을 삭제합니다.
     * @param projectId 프로젝트 ID
     * @param roleId 역할 ID (실제 문서 ID)
     * @return 작업 성공 여부
     */
    suspend fun deleteRole(projectId: String, roleId: String): Result<Unit>

    /**
     * 역할에 멤버를 추가하고 해당 역할의 memberCount를 1 증가시킵니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param roleId 역할 ID
     * @return 작업 성공 여부
     */
    suspend fun addMemberToRoleAndUpdateCount(projectId: String, userId: String, roleId: String): Result<Unit>

    /**
     * 역할에서 멤버를 제거하고 해당 역할의 memberCount를 1 감소시킵니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param roleId 역할 ID
     * @return 작업 성공 여부
     */
    suspend fun removeMemberFromRoleAndUpdateCount(projectId: String, userId: String, roleId: String): Result<Unit>
} 