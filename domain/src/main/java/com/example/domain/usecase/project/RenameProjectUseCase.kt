package com.example.domain.usecase.project

import com.example.domain.repository.ProjectRepository
import javax.inject.Inject

/**
 * 프로젝트 이름을 변경하는 유스케이스 인터페이스
 */
interface RenameProjectUseCase {
    suspend operator fun invoke(projectId: String, newName: String): Result<Unit>
}

/**
 * RenameProjectUseCase의 구현체
 * @param projectRepository 프로젝트 설정 관련 데이터 접근을 위한 Repository (가정)
 */
class RenameProjectUseCaseImpl @Inject constructor(
    private val projectRepository: ProjectRepository
) : RenameProjectUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트 이름을 변경합니다.
     * @param projectId 이름을 변경할 프로젝트의 ID
     * @param newName 변경할 새 이름
     * @return Result<Unit> 이름 변경 처리 결과
     */
    override suspend fun invoke(projectId: String, newName: String): Result<Unit> {
        projectRepository.updateProjectInfo(projectId, newName)
        return Result.success(Unit) // Return success for now
    }
} 