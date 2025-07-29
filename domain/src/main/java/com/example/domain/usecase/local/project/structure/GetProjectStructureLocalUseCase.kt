package com.example.domain.usecase.local.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.CategoryLocalRepository
import com.example.domain.repository.local.ProjectChannelLocalRepository
import javax.inject.Inject

interface GetProjectStructureLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId
    ): CustomResult<ProjectStructure, Exception>
}

class GetProjectStructureLocalUseCaseImpl @Inject constructor(
    private val categoryLocalRepository: CategoryLocalRepository,
    private val projectChannelLocalRepository: ProjectChannelLocalRepository
) : GetProjectStructureLocalUseCase {

    override suspend operator fun invoke(projectId: DocumentId): CustomResult<ProjectStructure, Exception> {
        return TODO("로컬 저장소에서 프로젝트의 전체 구조(카테고리 및 채널) 조회")
    }
} 