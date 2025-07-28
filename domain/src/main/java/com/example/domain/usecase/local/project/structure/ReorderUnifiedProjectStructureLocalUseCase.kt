package com.example.domain.usecase.local.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.CategoryLocalRepository
import com.example.domain.repository.local.ProjectChannelLocalRepository
import javax.inject.Inject

interface ReorderUnifiedProjectStructureLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        orderedStructure: List<Any> // Category 또는 Channel objects
    ): CustomResult<Unit, Exception>
}

class ReorderUnifiedProjectStructureLocalUseCaseImpl @Inject constructor(
    private val categoryLocalRepository: CategoryLocalRepository,
    private val projectChannelLocalRepository: ProjectChannelLocalRepository
) : ReorderUnifiedProjectStructureLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        orderedStructure: List<Any>
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 프로젝트 구조 전체를 재정렬")
    }
} 