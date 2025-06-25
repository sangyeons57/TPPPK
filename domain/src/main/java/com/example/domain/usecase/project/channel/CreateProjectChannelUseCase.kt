package com.example.domain.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.ProjectChannelRepository
import javax.inject.Inject

/**
 * 프로젝트의 특정 카테고리 내에 채널을 생성하는 UseCase입니다.
 *
 */
class CreateProjectChannelUseCase @Inject constructor(
    private val projectChannelRepository: ProjectChannelRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        name: Name,
        order: ProjectChannelOrder,
        channelType: ProjectChannelType
    ): CustomResult<Unit, Exception> {
        when (val result = authRepository.getCurrentUserSession()){
            is CustomResult.Success -> result.data
            is CustomResult.Failure -> return CustomResult.Failure(result.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(result.progress)
        }
        val projectChannel = ProjectChannel.create(
            channelName = name,
            order = order,
            channelType = channelType
        )

        return when (val result = projectChannelRepository.save(projectChannel)) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
} 