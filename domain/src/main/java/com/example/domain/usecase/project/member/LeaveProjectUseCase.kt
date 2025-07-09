package com.example.domain.usecase.project.member
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectRepository
import javax.inject.Inject

/**
 * 프로젝트 나가기 UseCase
 * 
 * 현재 사용자가 프로젝트에서 나가는 기능을 제공합니다.
 */
interface LeaveProjectUseCase {
    /**
     * 프로젝트에서 나갑니다.
     * 
     * @param projectId 나갈 프로젝트 ID
     * @return 나가기 결과
     */
    suspend operator fun invoke(projectId: DocumentId): CustomResult<Unit, Exception>
}

/**
 * 프로젝트 나가기 UseCase 구현체
 */
class LeaveProjectUseCaseImpl @Inject constructor(
    private val projectRepository: ProjectRepository
) : LeaveProjectUseCase {

    /**
     * 프로젝트에서 나갑니다.
     * 
     * @param projectId 나갈 프로젝트 ID
     * @return 나가기 결과
     */
    override suspend operator fun invoke(projectId: DocumentId): CustomResult<Unit, Exception> {
        return projectRepository.leaveProject(projectId)
    }
}