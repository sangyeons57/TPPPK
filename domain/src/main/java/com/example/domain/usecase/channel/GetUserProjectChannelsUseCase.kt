package com.example.domain.usecase.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.repository.ProjectChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자가 참여한 모든 채널 (DM, 프로젝트, 카테고리) 목록 스트림을 가져오는 유스케이스입니다.
 *
 * @property channelRepository 채널 관련 데이터를 제공하는 리포지토리
 */
class GetUserProjectChannelsUseCase @Inject constructor(
    private val channelRepository: ProjectChannelRepository
) {
    /**
     * 지정된 사용자가 참여한 모든 채널 목록 스트림을 반환합니다.
     * Repository에서 Flow<List<Channel>>을 받아 Flow<Result<List<Channel>>>로 변환합니다.
     *
     * @param userId 사용자의 ID
     * @return 채널 목록 또는 에러를 포함한 Result를 방출하는 [Flow]
     */
    operator fun invoke(userId: String): Flow<CustomResult<List<ProjectChannel>, Exception>> {
        // type = null 을 전달하여 모든 타입의 채널을 가져옵니다.
        return channelRepository.getProjectChannelsStream(userId)
        // 에러 처리는 Repository나 DataSource 단계에서 이루어지거나, ViewModel에서 .catch를 통해 처리합니다.
        // 여기서는 단순 매핑만 수행합니다.
    }
} 