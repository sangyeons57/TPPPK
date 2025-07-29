package com.example.domain.usecase.local.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ProjectChannelLocalRepository
import javax.inject.Inject

interface RenameChannelLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        channelId: DocumentId,
        newName: String
    ): CustomResult<Unit, Exception>
}

class RenameChannelLocalUseCaseImpl @Inject constructor(
    private val projectChannelLocalRepository: ProjectChannelLocalRepository
) : RenameChannelLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        channelId: DocumentId,
        newName: String
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 채널 이름 변경")
    }
} 