package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.base.DMChannelRepository
import javax.inject.Inject

/**
 * DM 채널을 생성하는 유스케이스입니다.
 */
class CreateDmChannelUseCase @Inject constructor(
    private val dmRepository: DMChannelRepository
) {
    suspend operator fun invoke(targetUserId: String): CustomResult<DMChannel, Exception> {
        if (targetUserId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Target user ID cannot be blank."))
        }
        val dmChannelId = dmRepository.createDmChannel(targetUserId)

        return when (dmChannelId) {
            is CustomResult.Success -> {
                return dmRepository.getDmChannelById(dmChannelId.data)
            }
            else -> {
                return CustomResult.Failure(Exception("Failed to create DM channel."))
            }
        }
    }
} 