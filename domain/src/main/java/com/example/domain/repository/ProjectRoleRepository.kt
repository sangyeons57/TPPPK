// 경로: domain/repository/ProjectRoleRepository.kt (신규 생성)
package com.example.domain.repository

import com.example.domain.model.Role
import com.example.domain.model.RolePermission
import kotlin.Result
import kotlinx.coroutines.flow.Flow // 필요시 Flow 사용 가능

/**
 * 프로젝트 역할 관련 데이터 처리를 위한 인터페이스
 */
interface ProjectRoleRepository {

    /**
     * 특정 프로젝트의 모든 역할 목록 가져오기
     * @param projectId 프로젝트 ID
     * @return 역할 목록 또는 에러
     */
    suspend fun getRoles(projectId: String): Result<List<Role>> // Flow를 사용하지 않는 경우
    fun getRolesStream(projectId: String): Flow<List<Role>> // 역할 목록 실시간 스트림 (권장)
    suspend fun fetchRoles(projectId: String): Result<Unit> // 역할 목록 새로고침

    /**
     * 특정 역할의 상세 정보(이름, 권한 맵) 가져오기
     * @param projectId 프로젝트 ID
     * @param roleId 역할 ID
     * @return 역할 정보 또는 에러
     */
    suspend fun getRoleDetails(projectId: String, roleId: String): Result<Role?>

    /**
     * 새 역할 생성
     * @param projectId 역할이 생성될 프로젝트 ID
     * @param name 새 역할 이름
     * @param permissions 새 역할의 권한 맵
     * @param isDefault 기본 역할 여부
     * @return 생성된 역할 ID 또는 에러
     */
    suspend fun createRole(projectId: String, name: String, permissions: Map<RolePermission, Boolean>, isDefault: Boolean): Result<String>

    /**
     * 기존 역할 업데이트 (이름 또는 권한)
     * @param projectId 프로젝트 ID
     * @param roleId 수정할 역할 ID
     * @param name 새 역할 이름
     * @param permissions 새 권한 맵
     * @param isDefault 기본 역할 여부 (null이면 변경하지 않음)
     * @return 성공/실패 결과
     */
    suspend fun updateRole(projectId: String, roleId: String, name: String, permissions: Map<RolePermission, Boolean>, isDefault: Boolean?): Result<Unit>

    /**
     * 역할 삭제
     * @param projectId 프로젝트 ID
     * @param roleId 삭제할 역할 ID
     * @return 성공/실패 결과
     */
    suspend fun deleteRole(projectId: String, roleId: String): Result<Unit>

    // TODO: 역할 순서 변경, 멤버에게 역할 할당/해제 등의 함수 필요 시 추가
}