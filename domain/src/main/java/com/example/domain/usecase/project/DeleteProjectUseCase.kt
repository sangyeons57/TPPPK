package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.repository.ProjectRepository
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.DMWrapperRepository
import com.example.domain.repository.ProjectsWrapperRepository
import javax.inject.Inject

/**
 * 프로젝트를 삭제하는 유스케이스 인터페이스
 */
interface DeleteProjectUseCase {
    suspend operator fun invoke(projectId: String): Result<Unit>
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
     * 유스케이스를 실행하여 프로젝트를 삭제합니다.
     * @param projectId 삭제할 프로젝트의 ID
     * @return Result<Unit> 삭제 처리 결과
     */
    override suspend fun invoke(projectId: String): Result<Unit> {
        // 현재 로그인된 사용자 ID 확인
        val currentUserSessionResult = authRepository.getCurrentUserSession()
        val currentUserSession = when (currentUserSessionResult) {
            is CustomResult.Success -> currentUserSessionResult.data
            is CustomResult.Failure -> return Result.failure(currentUserSessionResult.error)
            else -> return Result.failure(Exception("Unknown error :: getUeserId"))
        }

        when(val dmWrapperResult = projWrapperRepository.removeProjectFromUser(currentUserSession.userId, projectId)) {
            is CustomResult.Success -> Unit
            is CustomResult.Failure -> return Result.failure(dmWrapperResult.error)
            else -> return Result.failure(Exception("Unknown error :: deleteProjectWrapper"))
        }

        // 프로젝트 삭제 시도
        return when (val repoResult = projectRepository.deleteProject(projectId, currentUserSession.userId)) {
            is CustomResult.Success -> Result.success(Unit)
            is CustomResult.Failure -> Result.failure(repoResult.error)
            else -> Result.failure(Exception("Unknown error :: deleteProject"))
        }
    }
}