package com.example.domain.usecase.local.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.local.DMChannelLocalRepository
import javax.inject.Inject

interface GetDmChannelLocalUseCase {
    suspend operator fun invoke(dmChannelId: DocumentId): CustomResult<DMChannel, Exception>
    suspend fun findByOtherUserId(targetUserId: UserId): CustomResult<DMChannel, Exception>
}

class GetDmChannelLocalUseCaseImpl @Inject constructor(
    private val dmChannelLocalRepository: DMChannelLocalRepository
) : GetDmChannelLocalUseCase {

    override suspend operator fun invoke(dmChannelId: DocumentId): CustomResult<DMChannel, Exception> {
        return TODO("로컬 저장소에서 특정 DM 채널 정보 조회")
    }

    override suspend fun findByOtherUserId(targetUserId: UserId): CustomResult<DMChannel, Exception> {
        return TODO("로컬 저장소에서 특정 사용자와의 DM 채널 정보 조회")
    }
} 