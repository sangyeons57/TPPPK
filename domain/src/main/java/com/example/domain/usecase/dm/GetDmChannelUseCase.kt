package com.example.domain.usecase.dm

import com.example.domain.model.Channel
import com.example.domain.repository.DmRepository
import javax.inject.Inject

/**
 * 특정 사용자와의 DM 채널 정보를 가져오는 유스케이스입니다.
 */
class GetDmChannelUseCase @Inject constructor(
    private val dmRepository: DmRepository
) {
    suspend operator fun invoke(targetUserId: String): Result<Channel?> {
        if (targetUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("Target user ID cannot be blank."))
        }
        return dmRepository.getDmChannelWithUser(targetUserId)
    }
} 