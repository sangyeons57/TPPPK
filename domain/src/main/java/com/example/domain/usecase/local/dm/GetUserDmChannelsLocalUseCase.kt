package com.example.domain.usecase.local.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.local.AuthLocalRepository
import com.example.domain.repository.local.DMChannelLocalRepository
import com.example.domain.repository.local.DMWrapperLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetUserDmChannelsLocalUseCase {
    operator fun invoke(): Flow<CustomResult<List<DMChannel>, Exception>>
}

class GetUserDmChannelsLocalUseCaseImpl @Inject constructor(
    private val dmChannelLocalRepository: DMChannelLocalRepository,
    private val authRepository: AuthLocalRepository,
    private val dmWrapperLocalRepository: DMWrapperLocalRepository
) : GetUserDmChannelsLocalUseCase {

    override operator fun invoke(): Flow<CustomResult<List<DMChannel>, Exception>> {
        return TODO("로컬 저장소에서 현재 사용자의 모든 DM 채널 목록을 실시간으로 관찰")
    }
} 