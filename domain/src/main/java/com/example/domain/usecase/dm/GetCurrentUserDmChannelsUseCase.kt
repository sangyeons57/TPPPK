package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.base.DMChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 현재 사용자의 모든 DM 채널 목록을 스트림으로 가져오는 유스케이스입니다.
 */
class GetCurrentUserDmChannelsUseCase @Inject constructor(
    private val dmRepository: DMChannelRepository
) {
    suspend operator fun invoke(): Flow<CustomResult<List<DMChannel>, Exception>> {
        return dmRepository.observeAll().map { result ->
            when(result) {
                is CustomResult.Success -> CustomResult.Success(result.data.map { it as DMChannel })
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }
} 