package com.example.domain.usecase.project

import com.example.domain.repository.ProjectMemberRepository
import javax.inject.Inject

/**
 * 프로젝트 멤버의 역할을 업데이트하는 유스케이스 인터페이스
 */
interface UpdateMemberRolesUseCase {
    suspend operator fun invoke(projectId: String, userId: String, roleIds: List<String>): Result<Unit>
}

/**
 * UpdateMemberRolesUseCase의 구현체
 * @param projectMemberRepository 프로젝트 멤버 데이터 접근을 위한 Repository
 */
class UpdateMemberRolesUseCaseImpl @Inject constructor(
    private val projectMemberRepository: ProjectMemberRepository
) : UpdateMemberRolesUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트 멤버의 역할을 업데이트합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param roleIds 업데이트할 역할 ID 목록
     * @return Result<Unit> 업데이트 처리 결과
     */
    override suspend fun invoke(projectId: String, userId: String, rolesId: List<String>): Result<Unit> {
        return projectMemberRepository.updateMemberRoles(projectId, userId, rolesId)
    }
} 