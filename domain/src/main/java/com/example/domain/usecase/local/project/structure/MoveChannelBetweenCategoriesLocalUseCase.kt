package com.example.domain.usecase.local.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.CategoryLocalRepository
import com.example.domain.repository.local.ProjectChannelLocalRepository
import javax.inject.Inject

interface MoveChannelBetweenCategoriesLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        channelId: DocumentId,
        fromCategoryId: DocumentId,
        toCategoryId: DocumentId,
        newPosition: Int
    ): CustomResult<Unit, Exception>
}

class MoveChannelBetweenCategoriesLocalUseCaseImpl @Inject constructor(
    private val projectChannelLocalRepository: ProjectChannelLocalRepository,
    private val categoryLocalRepository: CategoryLocalRepository
) : MoveChannelBetweenCategoriesLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        channelId: DocumentId,
        fromCategoryId: DocumentId,
        toCategoryId: DocumentId,
        newPosition: Int
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 채널을 다른 카테고리로 이동")
    }
}