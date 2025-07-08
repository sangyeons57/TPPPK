package com.example.domain.usecase.project.core

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.base.ProjectsWrapperRepository
import javax.inject.Inject

/**
 * 프로젝트를 삭제하는 유스케이스 인터페이스
 */
interface DeleteProjectUseCase {
    suspend operator fun invoke(projectId: DocumentId): CustomResult<Unit, Exception>
}

/**
 * DeleteProjectUseCase의 구현체
 * @param projectRepository 프로젝트 관련 데이터 접근을 위한 Repository
 * @param authRepository 인증 관련 데이터 접근을 위한 Repository
 */
class DeleteProjectUseCaseImpl @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val projWrapperRepository: ProjectsWrapperRepository,
) : DeleteProjectUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트를 소프트 삭제합니다.
     * Firebase Function을 호출하여 프로젝트 상태를 DELETED로 변경합니다.
     * @param projectId 삭제할 프로젝트의 ID
     * @return CustomResult<Unit, Exception> 삭제 처리 결과
     */
    override suspend fun invoke(projectId: DocumentId): CustomResult<Unit, Exception> {
        return when (val result = projectRepository.deleteProject(projectId)) {
            is CustomResult.Success -> {
                // Firebase Function 호출이 성공하면 Unit을 반환
                CustomResult.Success(Unit)
            }
            is CustomResult.Failure -> {
                // Firebase Function 호출이 실패하면 에러를 전달
                CustomResult.Failure(result.error)
            }
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
            is CustomResult.Initial -> CustomResult.Initial
        }
    }
}