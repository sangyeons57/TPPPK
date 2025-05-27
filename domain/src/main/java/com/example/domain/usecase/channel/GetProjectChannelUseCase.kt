// TODO: ProjectChannelRepository is a required new interface and needs to define the method getProjectChannelDetailsStream(channelId: String): Flow<Result<ProjectChannelModel>>
// TODO: A specific model ProjectChannelModel might be more appropriate
package com.example.domain.usecase.channel

import com.example.domain.model.Channel // TODO: Replace with ProjectChannelModel if defined
import com.example.domain.repository.ProjectChannelRepository // TODO: Create this repository interface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

/**
 * 특정 프로젝트 채널의 정보 스트림을 가져오는 유스케이스입니다.
 *
 * @property projectChannelRepository 프로젝트 채널 관련 데이터를 제공하는 리포지토리
 */
class GetProjectChannelUseCase @Inject constructor(
    private val projectChannelRepository: ProjectChannelRepository
) {
    /**
     * 지정된 ID의 프로젝트 채널 정보 스트림을 반환합니다.
     * Repository에서 Flow<Channel>을 받아 Flow<Result<Channel>>로 변환합니다.
     * // TODO: Replace Channel with ProjectChannelModel if defined
     *
     * @param channelId 채널의 ID
     * @return 프로젝트 채널 정보 또는 에러를 포함한 Result를 방출하는 [Flow]
     */
    operator fun invoke(channelId: String): Flow<Result<Channel>> { // TODO: Replace Channel with ProjectChannelModel
        return projectChannelRepository.getProjectChannelDetailsStream(channelId)
            .map { channel -> Result.success(channel) }
        // 에러 처리는 Repository나 DataSource 단계에서 이루어지거나, ViewModel에서 .catch를 통해 처리합니다.
    }
}