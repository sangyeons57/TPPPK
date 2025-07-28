package com.example.domain.usecase.local.project.category

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.CategoryLocalRepository
import javax.inject.Inject

interface UpdateCategoryLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        category: Category
    ): CustomResult<Category, Exception>
}

class UpdateCategoryLocalUseCaseImpl @Inject constructor(
    private val categoryLocalRepository: CategoryLocalRepository
) : UpdateCategoryLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        category: Category
    ): CustomResult<Category, Exception> {
        return TODO("로컬 저장소에서 카테고리 정보 업데이트")
    }
} 