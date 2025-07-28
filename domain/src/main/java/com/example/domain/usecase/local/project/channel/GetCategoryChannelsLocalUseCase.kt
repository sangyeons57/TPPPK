package com.example.domain.usecase.local.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ProjectChannelLocalRepository
import javax.inject.Inject

interface GetCategoryChannelsLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId
    ): CustomResult<List<ProjectChannel>, Exception>
}

class GetCategoryChannelsLocalUseCaseImpl @Inject constructor(
    private val projectChannelLocalRepository: ProjectChannelLocalRepository
) : GetCategoryChannelsLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId
    ): CustomResult<List<ProjectChannel>, Exception> {
        return TODO("로컬 저장소에서 특정 카테고리의 채널 목록 조회")
    }
} 