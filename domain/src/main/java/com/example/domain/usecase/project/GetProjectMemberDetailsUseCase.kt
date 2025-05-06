package com.example.domain.usecase.project

import com.example.domain.model.ProjectMember
import com.example.domain.repository.ProjectMemberRepository
import javax.inject.Inject

/**
 * 특정 프로젝트 멤버의 상세 정보를 가져오는 유스케이스 인터페이스
 */
interface GetProjectMemberDetailsUseCase {
    suspend operator fun invoke(projectId: String, userId: String): Result<ProjectMember?>
}

/**
 * GetProjectMemberDetailsUseCase의 구현체
 * @param projectMemberRepository 프로젝트 멤버 데이터 접근을 위한 Repository
 */
class GetProjectMemberDetailsUseCaseImpl @Inject constructor(
    private val projectMemberRepository: ProjectMemberRepository
) : GetProjectMemberDetailsUseCase {

    /**
     * 유스케이스를 실행하여 특정 프로젝트 멤버의 상세 정보를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return Result<ProjectMember> 멤버 정보 로드 결과
     */
    override suspend fun invoke(projectId: String, userId: String): Result<ProjectMember?> {
        return projectMemberRepository.getProjectMember(projectId, userId)
    }
} 