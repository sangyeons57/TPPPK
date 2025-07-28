package com.example.domain.usecase.local.dev

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.NotificationLocalRepository
import javax.inject.Inject

interface SendLocalTestNotificationUseCase {
    suspend operator fun invoke(
        userId: String,
        channelId: String
    ): CustomResult<Map<String, Any?>, Exception>
}

class SendLocalTestNotificationUseCaseImpl @Inject constructor(
    private val notificationLocalRepository: NotificationLocalRepository
) : SendLocalTestNotificationUseCase {

    override suspend operator fun invoke(
        userId: String,
        channelId: String
    ): CustomResult<Map<String, Any?>, Exception> {
        return TODO("로컬 저장소에서 테스트 알림을 생성하고 결과 반환")
    }
} 