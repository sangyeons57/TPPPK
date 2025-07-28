package com.example.domain.usecase.local.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ProjectChannelLocalRepository
import javax.inject.Inject

interface CreateProjectChannelLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        channelName: String,
        channelDescription: String? = null
    ): CustomResult<ProjectChannel, Exception>
}

class CreateProjectChannelLocalUseCaseImpl @Inject constructor(
    private val projectChannelLocalRepository: ProjectChannelLocalRepository
) : CreateProjectChannelLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        channelName: String,
        channelDescription: String?
    ): CustomResult<ProjectChannel, Exception> {
        return TODO("로컬 저장소에 새 프로젝트 채널 생성")
    }
} 