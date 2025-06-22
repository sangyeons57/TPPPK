package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.ProjectsWrapperRepository
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
        TODO("Not yet implemented [Firebase Function에서 구현]")
    }
}