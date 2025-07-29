package com.example.domain.usecase.local.project.category

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.CategoryLocalRepository
import javax.inject.Inject

interface DeleteCategoryLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId
    ): CustomResult<Unit, Exception>
}

class DeleteCategoryLocalUseCaseImpl @Inject constructor(
    private val categoryLocalRepository: CategoryLocalRepository
) : DeleteCategoryLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 카테고리 삭제")
    }
} 