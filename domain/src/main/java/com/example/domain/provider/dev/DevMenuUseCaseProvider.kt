package com.example.domain.provider.dev

import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.base.User
import com.example.domain.repository.remote.DefaultRepository
import com.example.domain.usecase.dev.SendFcmTestNotificationUseCase
import com.example.domain.usecase.dev.SendFcmTestNotificationUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevMenuUseCaseProvider @Inject constructor(
    private val userRepository: DefaultRepository<User>
) {
    fun create(): DevMenuUseCases {
        userRepository.setCollection(CollectionPath.users)
        return DevMenuUseCases(
            userRepository = userRepository,

            sendFcmTestNotificationUseCase = SendFcmTestNotificationUseCaseImpl(userRepository),
        )
    }
}

data class DevMenuUseCases(
    val sendFcmTestNotificationUseCase: SendFcmTestNotificationUseCase,
    val userRepository: DefaultRepository<User>
) 