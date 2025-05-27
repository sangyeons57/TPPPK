package com.example.domain.usecase.project

import com.example.domain._repository.ProjectRepository // Corrected import path
import javax.inject.Inject

/**
 * 프로젝트를 삭제하는 유스케이스 인터페이스
 */
interface DeleteProjectUseCase {
    suspend operator fun invoke(projectId: String, currentUserId: String): Result<Unit>
}

/**
 * DeleteProjectUseCase의 구현체. This Usecase now relies on ProjectRepository.
 * The ProjectRepository.deleteProject method requires currentUserId for authorization.
 * @param projectRepository 프로젝트 관련 데이터 접근을 위한 Repository.
 */
class DeleteProjectUseCaseImpl @Inject constructor(
    private val projectRepository: ProjectRepository
) : DeleteProjectUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트를 삭제합니다.
     * @param projectId 삭제할 프로젝트의 ID
     * @param currentUserId 현재 작업을 요청하는 사용자의 ID (권한 검증용)
     * @return Result<Unit> 삭제 처리 결과
     */
    override suspend fun invoke(projectId: String, currentUserId: String): Result<Unit> {
        return projectRepository.deleteProject(projectId, currentUserId)
    }
}