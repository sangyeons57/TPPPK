package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.DMChannelRepository
import javax.inject.Inject

/**
 * 특정 사용자와의 DM 채널 ID를 가져오는 UseCase
 * 
 * @property dmRepository DM 채널 관련 기능을 제공하는 Repository
 */
class GetDmChannelUseCase @Inject constructor(
    private val dmRepository: DMChannelRepository
) {
    suspend operator fun invoke(dmChannelId: DocumentId): CustomResult<DMChannel, Exception> {
        if (dmChannelId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Target user ID cannot be blank."))
        }
        return dmRepository.findById(dmChannelId).successProcess {
            it as DMChannel
        }
    }

    suspend fun findByOtherUserId(targetUserId: UserId): CustomResult<DMChannel, Exception> {
        if (targetUserId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Target user ID cannot be blank."))
        }
        return dmRepository.findByOtherUserId(targetUserId.value)
    }
} 