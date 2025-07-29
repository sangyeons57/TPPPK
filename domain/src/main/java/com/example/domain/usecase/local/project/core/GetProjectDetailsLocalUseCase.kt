package com.example.domain.usecase.local.project.core

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ProjectLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetProjectDetailsLocalUseCase {
    suspend operator fun invoke(projectId: DocumentId): CustomResult<Project, Exception>
}

interface GetProjectDetailsStreamLocalUseCase {
    operator fun invoke(projectId: DocumentId): Flow<CustomResult<Project, Exception>>
}

class GetProjectDetailsLocalUseCaseImpl @Inject constructor(
    private val projectLocalRepository: ProjectLocalRepository
) : GetProjectDetailsLocalUseCase {

    override suspend operator fun invoke(projectId: DocumentId): CustomResult<Project, Exception> {
        return TODO("로컬 저장소에서 특정 프로젝트의 상세 정보 조회")
    }
}

class GetProjectDetailsStreamLocalUseCaseImpl @Inject constructor(
    private val projectLocalRepository: ProjectLocalRepository
) : GetProjectDetailsStreamLocalUseCase {

    override operator fun invoke(projectId: DocumentId): Flow<CustomResult<Project, Exception>> {
        return TODO("로컬 저장소에서 특정 프로젝트의 상세 정보를 실시간으로 관찰")
    }
} 