package com.example.domain.usecase.project

import com.example.domain.model.RolePermission
import com.example.domain.repository.ProjectRoleRepository
import javax.inject.Inject

/**
 * 새로운 역할을 생성하는 유스케이스 인터페이스
 */
interface CreateRoleUseCase {
    suspend operator fun invoke(projectId: String, roleName: String, permissions: Map<RolePermission, Boolean>): Result<Unit> // 생성 성공 시 생성된 역할 ID를 반환할 수도 있음
}

/**
 * CreateRoleUseCase의 구현체
 * @param projectRoleRepository 프로젝트 역할 데이터 접근을 위한 Repository
 */
class CreateRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : CreateRoleUseCase {

    /**
     * 유스케이스를 실행하여 새로운 역할을 생성합니다.
     * @param projectId 프로젝트 ID
     * @param roleName 생성할 역할의 이름
     * @param permissions 생성할 역할의 권한 맵
     * @return Result<Unit> 역할 생성 처리 결과
     */
    override suspend fun invoke(projectId: String, roleName: String, permissions: Map<RolePermission, Boolean>): Result<Unit> {
        return projectRoleRepository.createRole(projectId, roleName, permissions)
    }
} 