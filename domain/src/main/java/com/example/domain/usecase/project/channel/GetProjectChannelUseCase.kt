package com.example.domain.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.repository.ProjectChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

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
    operator fun invoke(projectId: String, channelId: String): Flow<CustomResult<List<ProjectChannel>, Exception>> {
        return projectChannelRepository.getProjectChannelsStream(projectId, channelId)
        // 에러 처리는 Repository나 DataSource 단계에서 이루어지거나, ViewModel에서 .catch를 통해 처리합니다.
    }
} 