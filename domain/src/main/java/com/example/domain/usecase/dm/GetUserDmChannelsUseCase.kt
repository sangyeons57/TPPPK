package com.example.domain.usecase.dm

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.DMChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 현재 로그인한 사용자의 DM 채널 목록을 스트림으로 가져오는 UseCase
 */
class GetUserDmChannelsUseCase @Inject constructor(
    private val dmChannelRepository: DMChannelRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 현재 로그인한 사용자의 모든 DM 채널을 Flow로 반환합니다.
     * 
     * @return DM 채널 목록을 포함하는 Flow
     */
    suspend operator fun invoke(): Flow<CustomResult<List<DMChannel>, Exception>>{

        // 현재 사용자의 DM 채널 스트림 수집
        return dmChannelRepository.getCurrentDmChannelsStream()
    }
} 