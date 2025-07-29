package com.example.domain.usecase.local.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.LocalDMChannelRepository
import com.example.domain.repository.remote.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface BlockDMChannelLocalUseCase {
    operator fun invoke(channelId: DocumentId): Flow<CustomResult<DocumentId, Exception>>
}

class BlockDMChannelLocalUseCaseImpl @Inject constructor(
    private val dmChannelLocalRepository: LocalDMChannelRepository,
    private val authRepository: AuthRepository
) : BlockDMChannelLocalUseCase {

    override operator fun invoke(channelId: DocumentId): Flow<CustomResult<DocumentId, Exception>> {
        return TODO("로컬 저장소에서 DM 채널을 차단하고 DMWrapper를 제거")
    }
} 