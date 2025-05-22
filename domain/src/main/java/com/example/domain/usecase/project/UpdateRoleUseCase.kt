package com.example.domain.usecase.project

import com.example.domain.model.RolePermission
import com.example.domain.repository.ProjectRoleRepository
import javax.inject.Inject

/**
 * 기존 역할을 수정하는 유스케이스 인터페이스
 */
interface UpdateRoleUseCase {
    suspend operator fun invoke(roleId: String, roleName: String, permissions: Map<RolePermission, Boolean>): Result<Unit>
}

/**
 * UpdateRoleUseCase의 구현체
 * @param projectRoleRepository 프로젝트 역할 데이터 접근을 위한 Repository
 */
class UpdateRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : UpdateRoleUseCase {

    /**
     * 유스케이스를 실행하여 기존 역할을 수정합니다.
     * @param roleId 수정할 역할의 ID
     * @param roleName 수정할 역할의 새 이름
     * @param permissions 수정할 역할의 새 권한 맵
     * @return Result<Unit> 역할 수정 처리 결과
     */
    override suspend fun invoke(roleId: String, roleName: String, permissions: Map<RolePermission, Boolean>): Result<Unit> {
        return projectRoleRepository.updateRole(roleId, roleName, permissions)
    }
} 