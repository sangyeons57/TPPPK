package com.example.domain.usecase.local.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ProjectChannelLocalRepository
import javax.inject.Inject

interface ReorderChannelsLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        channelIds: List<DocumentId>
    ): CustomResult<Unit, Exception>
}

class ReorderChannelsLocalUseCaseImpl @Inject constructor(
    private val projectChannelLocalRepository: ProjectChannelLocalRepository
) : ReorderChannelsLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        channelIds: List<DocumentId>
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 채널들의 순서를 재정렬")
    }
} 