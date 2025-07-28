package com.example.domain.usecase.local.project.category

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.CategoryLocalRepository
import javax.inject.Inject

interface GetCategoryDetailsLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId
    ): CustomResult<Category, Exception>
}

class GetCategoryDetailsLocalUseCaseImpl @Inject constructor(
    private val categoryLocalRepository: CategoryLocalRepository
) : GetCategoryDetailsLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId
    ): CustomResult<Category, Exception> {
        return TODO("로컬 저장소에서 특정 카테고리의 상세 정보 조회")
    }
} 