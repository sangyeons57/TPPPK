package com.example.domain.usecase.local.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ProjectChannelLocalRepository
import javax.inject.Inject

interface DeleteChannelLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        channelId: DocumentId
    ): CustomResult<Unit, Exception>
}

class DeleteChannelLocalUseCaseImpl @Inject constructor(
    private val projectChannelLocalRepository: ProjectChannelLocalRepository
) : DeleteChannelLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        channelId: DocumentId
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 채널 삭제")
    }
} 