package com.example.domain.usecase.dm

import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 사용자의 DM 채널 목록 스트림을 가져오는 유스케이스입니다.
 *
 * @property channelRepository 채널 관련 데이터를 제공하는 리포지토리
 */
class GetUserDmChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) {
    /**
     * 지정된 사용자의 DM 채널 목록 스트림을 반환합니다.
     *
     * @param userId 사용자의 ID
     * @return DM 채널 목록을 방출하는 [Flow]
     */
    operator fun invoke(userId: String): Flow<List<Channel>> {
        return channelRepository.getUserChannelsStream(userId, ChannelType.DM)
    }
} 