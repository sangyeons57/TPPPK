package com.example.domain.usecase.local.projects

import com.example.domain.repository.local.ProjectLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface DeleteProjectUseCase {
    suspend operator fun invoke(projectId: String): CustomResult<Unit, Exception>
}

class DeleteProjectUseCaseImpl @Inject constructor(
    private val projectLocalRepository: ProjectLocalRepository
) : DeleteProjectUseCase {

    override suspend operator fun invoke(projectId: String): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에서 특정 프로젝트를 삭제")
    }
} 