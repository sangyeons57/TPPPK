package com.example.domain.usecase.project.core

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.repository.base.ProjectRepository
import javax.inject.Inject

/**
 * 프로젝트 이름을 변경하는 유스케이스 인터페이스
 */
interface RenameProjectUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        newName: ProjectName
    ): CustomResult<Unit, Exception>
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
    override suspend fun invoke(
        projectId: DocumentId,
        newName: ProjectName
    ): CustomResult<Unit, Exception> {
        val project = when (val projectResult = projectRepository.findById(projectId)) {
            is CustomResult.Success -> projectResult.data
            is CustomResult.Failure -> return CustomResult.Failure(projectResult.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(projectResult.progress)
        } as Project

        project.changeName(newName)

        return when (val saveResult = projectRepository.save(project)) {
            is CustomResult.Success -> {
                EventDispatcher.publish(project)
                CustomResult.Success(Unit)
            }
            is CustomResult.Failure -> CustomResult.Failure(saveResult.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(saveResult.progress)
        }
    }
} 