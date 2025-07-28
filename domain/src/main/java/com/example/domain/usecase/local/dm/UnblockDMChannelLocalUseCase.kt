package com.example.domain.usecase.local.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.AuthLocalRepository
import com.example.domain.repository.local.DMChannelLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface UnblockDMChannelLocalUseCase {
    operator fun invoke(channelId: DocumentId): Flow<CustomResult<Map<String, Any?>, Exception>>
    operator fun invoke(targetUserName: UserName): Flow<CustomResult<Map<String, Any?>, Exception>>
}

class UnblockDMChannelLocalUseCaseImpl @Inject constructor(
    private val dmChannelLocalRepository: DMChannelLocalRepository,
    private val authRepository: AuthLocalRepository
) : UnblockDMChannelLocalUseCase {

    override operator fun invoke(channelId: DocumentId): Flow<CustomResult<Map<String, Any?>, Exception>> {
        return TODO("로컬 저장소에서 DM 채널 차단을 해제하고 필요시 DMWrapper를 재생성")
    }

    override operator fun invoke(targetUserName: UserName): Flow<CustomResult<Map<String, Any?>, Exception>> {
        return TODO("로컬 저장소에서 사용자 이름을 통해 DM 채널 차단 해제")
    }
} 