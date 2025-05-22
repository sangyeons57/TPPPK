package com.example.domain.usecase.dm

import com.example.domain.model.Channel
import com.example.domain.repository.DmRepository
import javax.inject.Inject

/**
 * DM 채널을 생성하는 유스케이스입니다.
 */
class CreateDmChannelUseCase @Inject constructor(
    private val dmRepository: DmRepository
) {
    suspend operator fun invoke(targetUserId: String, channelName: String? = null): Result<Channel> {
        if (targetUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("Target user ID cannot be blank."))
        }
        return dmRepository.createDmChannel(targetUserId, channelName)
    }
} 