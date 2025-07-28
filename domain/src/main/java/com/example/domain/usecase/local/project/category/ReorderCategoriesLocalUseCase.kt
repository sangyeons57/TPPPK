package com.example.domain.usecase.local.project.category

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.CategoryLocalRepository
import javax.inject.Inject

interface ReorderCategoriesLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryIds: List<DocumentId>
    ): CustomResult<Unit, Exception>
}

class ReorderCategoriesLocalUseCaseImpl @Inject constructor(
    private val categoryLocalRepository: CategoryLocalRepository
) : ReorderCategoriesLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryIds: List<DocumentId>
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 카테고리들의 순서를 재정렬")
    }
} 