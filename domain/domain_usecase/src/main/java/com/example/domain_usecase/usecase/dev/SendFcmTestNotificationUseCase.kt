package com.example.domain_usecase.usecase.dev

import com.example.core_common.result.CustomResult
import com.example.domain_repository.base.UserRepository

interface SendFcmTestNotificationUseCase {
    suspend operator fun invoke(
        userId: String,
        channelId: String
    ): CustomResult<Map<String, Any?>, Exception>
}

class SendFcmTestNotificationUseCaseImpl(
    private val userRepository: UserRepository
) : SendFcmTestNotificationUseCase {
    override suspend fun invoke(
        userId: String,
        channelId: String
    ): CustomResult<Map<String, Any?>, Exception> {
        return userRepository.sendFcmTestNotification(userId, channelId)
    }
} 