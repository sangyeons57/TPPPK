package com.example.domain.usecase.project

import com.example.domain.model.RolePermission
import com.example.domain.repository.ProjectRoleRepository
import javax.inject.Inject

/**
 * 특정 역할의 상세 정보(이름, 권한)를 가져오는 유스케이스 인터페이스
 */
interface GetRoleDetailsUseCase {
    // 역할 이름과 권한 맵을 Pair로 반환
    suspend operator fun invoke(projectId: String, roleId: String): Result<com.example.domain.model.Role?>
}

/**
 * GetRoleDetailsUseCase의 구현체
 * @param projectRoleRepository 프로젝트 역할 데이터 접근을 위한 Repository
 */
class GetRoleDetailsUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : GetRoleDetailsUseCase {

    /**
     * 유스케이스를 실행하여 특정 역할의 상세 정보를 가져옵니다.
     * @param roleId 역할 ID
     * @return Result<Pair<String, Map<RolePermission, Boolean>>> 역할 상세 정보 로드 결과
     */
    override suspend fun invoke(projectId: String, roleId: String): Result<com.example.domain.model.Role?> {
        return projectRoleRepository.getRoleDetails(projectId, roleId)
    }
}