package com.example.domain.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 특정 채널의 정보 스트림을 가져오는 유스케이스입니다.
 *
 * @property projectChannelRepository 채널 관련 데이터를 제공하는 리포지토리
 */
class GetProjectChannelUseCase @Inject constructor(
    private val projectChannelRepository: ProjectChannelRepository
) {
    /**
     * 지정된 ID의 채널 정보 스트림을 반환합니다.
     * Repository에서 Flow<Channel>을 받아 Flow<Result<Channel>>로 변환합니다.
     *
     * @param channelId 채널의 ID
     * @return 채널 정보 또는 에러를 포함한 Result를 방출하는 [Flow]
     */
    suspend operator fun invoke(channelId: String): Flow<CustomResult<ProjectChannel, Exception>> {
        return when (val projectChannelResult= projectChannelRepository.observe(DocumentId.from(channelId)).first()) {
            is CustomResult.Success -> flowOf(CustomResult.Success(projectChannelResult.data as ProjectChannel))
            is CustomResult.Failure -> flowOf(CustomResult.Failure(projectChannelResult.error))
            is CustomResult.Initial -> flowOf(CustomResult.Initial)
            is CustomResult.Loading -> flowOf(CustomResult.Loading)
            is CustomResult.Progress -> flowOf(CustomResult.Progress(projectChannelResult.progress))
        }
    }
} 