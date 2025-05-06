package com.example.domain.usecase.project

import com.example.domain.model.Role
import com.example.domain.repository.ProjectRoleRepository
import javax.inject.Inject

/**
 * 특정 프로젝트의 전체 역할 목록을 가져오는 유스케이스 인터페이스
 */
interface GetProjectRolesUseCase {
    suspend operator fun invoke(projectId: String): Result<List<Role>>
}

/**
 * GetProjectRolesUseCase의 구현체
 * @param projectRoleRepository 프로젝트 역할 데이터 접근을 위한 Repository
 */
class GetProjectRolesUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : GetProjectRolesUseCase {

    /**
     * 유스케이스를 실행하여 특정 프로젝트의 전체 역할 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return Result<List<Role>> 역할 목록 로드 결과
     */
    override suspend fun invoke(projectId: String): Result<List<Role>> {
        return projectRoleRepository.getRoles(projectId)
    }
} 