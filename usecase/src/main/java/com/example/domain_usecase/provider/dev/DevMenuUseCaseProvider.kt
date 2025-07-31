package com.example.domain_usecase.provider.dev

import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.UserRepository
import com.example.domain_usecase.usecase.dev.SendFcmTestNotificationUseCase
import com.example.domain_usecase.usecase.dev.SendFcmTestNotificationUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevMenuUseCaseProvider @Inject constructor(
    private val userRepository: UserRepository
) {
    fun create(): DevMenuUseCases {
        userRepository.setCollection(CollectionPath.users)
        return DevMenuUseCases(
            sendFcmTestNotificationUseCase = SendFcmTestNotificationUseCaseImpl(this.userRepository)
        )
    }
}

data class DevMenuUseCases(
    val sendFcmTestNotificationUseCase: SendFcmTestNotificationUseCase
) 