package com.example.domain.usecase.local.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ProjectChannelLocalRepository
import javax.inject.Inject

interface UpdateProjectChannelLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        channel: ProjectChannel
    ): CustomResult<ProjectChannel, Exception>
}

class UpdateProjectChannelLocalUseCaseImpl @Inject constructor(
    private val projectChannelLocalRepository: ProjectChannelLocalRepository
) : UpdateProjectChannelLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        channel: ProjectChannel
    ): CustomResult<ProjectChannel, Exception> {
        return TODO("로컬 저장소에서 프로젝트 채널 정보 업데이트")
    }
} 