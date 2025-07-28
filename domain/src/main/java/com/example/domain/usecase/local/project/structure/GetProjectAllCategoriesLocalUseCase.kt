package com.example.domain.usecase.local.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.CategoryLocalRepository
import javax.inject.Inject

interface GetProjectAllCategoriesLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId
    ): CustomResult<List<Category>, Exception>
}

class GetProjectAllCategoriesLocalUseCaseImpl @Inject constructor(
    private val categoryLocalRepository: CategoryLocalRepository
) : GetProjectAllCategoriesLocalUseCase {

    override suspend operator fun invoke(projectId: DocumentId): CustomResult<List<Category>, Exception> {
        return TODO("로컬 저장소에서 프로젝트의 모든 카테고리 조회")
    }
} 