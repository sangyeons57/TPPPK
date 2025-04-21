// 경로: domain/repository/ProjectRoleRepository.kt (신규 생성)
package com.example.teamnovapersonalprojectprojectingkotlin.domain.repository

import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.Role // 생성한 Role 모델 import
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.RolePermission // RolePermission enum import
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
     * @param roleId 역할 ID
     * @return 역할 이름과 권한 맵 Pair 또는 에러
     */
    suspend fun getRoleDetails(roleId: String): Result<Pair<String, Map<RolePermission, Boolean>>>

    /**
     * 새 역할 생성
     * @param projectId 역할이 생성될 프로젝트 ID
     * @param name 새 역할 이름
     * @param permissions 새 역할의 권한 맵
     * @return 성공/실패 결과 (생성된 Role 객체를 반환할 수도 있음)
     */
    suspend fun createRole(projectId: String, name: String, permissions: Map<RolePermission, Boolean>): Result<Unit> // 또는 Result<Role>

    /**
     * 기존 역할 업데이트 (이름 또는 권한)
     * @param roleId 수정할 역할 ID
     * @param name 새 역할 이름
     * @param permissions 새 권한 맵
     * @return 성공/실패 결과
     */
    suspend fun updateRole(roleId: String, name: String, permissions: Map<RolePermission, Boolean>): Result<Unit>

    /**
     * 역할 삭제
     * @param roleId 삭제할 역할 ID
     * @return 성공/실패 결과
     */
    suspend fun deleteRole(roleId: String): Result<Unit>

    // TODO: 역할 순서 변경, 멤버에게 역할 할당/해제 등의 함수 필요 시 추가
}