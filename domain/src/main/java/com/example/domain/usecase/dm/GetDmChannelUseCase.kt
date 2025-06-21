package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.DMChannelRepository
import javax.inject.Inject

/**
 * 특정 사용자와의 DM 채널 정보를 가져오는 유스케이스입니다.
 */
class GetDmChannelUseCase @Inject constructor(
    private val dmRepository: DMChannelRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(targetUserId: String): CustomResult<DMChannel, Exception> {
        if (targetUserId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Target user ID cannot be blank."))
        }
        val currentUserSessionResult = authRepository.getCurrentUserSession()

        return when (currentUserSessionResult) {
            is CustomResult.Success -> {
                val currentUserSession = currentUserSessionResult.data
                val userIds : List<String> = listOf(currentUserSession.userId, targetUserId)
                return dmRepository.findByOtherUserId(userIds)
            }
            is CustomResult.Failure -> {
                return CustomResult.Failure(currentUserSessionResult.error)
            }
            else -> {
                return CustomResult.Failure(Exception("Unknown error occurred."))
            }
        }

    }
} 