package com.example.domain.usecase.local.projects

import com.example.domain.model.Project
import com.example.domain.repository.local.ProjectLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetProjectUseCase {
    operator fun invoke(projectId: String): Flow<Project?>
}

class GetProjectUseCaseImpl @Inject constructor(
    private val projectLocalRepository: ProjectLocalRepository
) : GetProjectUseCase {

    override operator fun invoke(projectId: String): Flow<Project?> {
        return TODO("특정 프로젝트 정보를 실시간으로 관찰")
    }
} 