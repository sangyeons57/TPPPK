package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.DMChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 현재 사용자의 모든 DM 채널 목록을 스트림으로 가져오는 유스케이스입니다.
 */
class GetCurrentUserDmChannelsUseCase @Inject constructor(
    private val dmRepository: DMChannelRepository
) {
    suspend operator fun invoke(): Flow<CustomResult<List<DMChannel>, Exception>> {
        return dmRepository.getCurrentDmChannelsStream()
    }
} 