package com.example.domain.usecase.local.project.category

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.CategoryLocalRepository
import javax.inject.Inject

interface RenameCategoryLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        newName: String
    ): CustomResult<Unit, Exception>
}

class RenameCategoryLocalUseCaseImpl @Inject constructor(
    private val categoryLocalRepository: CategoryLocalRepository
) : RenameCategoryLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        newName: String
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 카테고리 이름 변경")
    }
} 