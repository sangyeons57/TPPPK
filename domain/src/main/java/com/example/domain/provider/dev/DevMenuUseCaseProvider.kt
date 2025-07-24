package com.example.domain.provider.dev

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.example.domain.usecase.dev.SendFcmTestNotificationUseCase
import com.example.domain.usecase.dev.SendFcmTestNotificationUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevMenuUseCaseProvider @Inject constructor(
    private val userRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<UserRepositoryFactoryContext, UserRepository>
) {
    fun create(): DevMenuUseCases {
        val userRepository = userRepositoryFactory.create(
            UserRepositoryFactoryContext(
                collectionPath = CollectionPath.users
            )
        )
        return DevMenuUseCases(
            sendFcmTestNotificationUseCase = SendFcmTestNotificationUseCaseImpl(userRepository)
        )
    }
}

data class DevMenuUseCases(
    val sendFcmTestNotificationUseCase: SendFcmTestNotificationUseCase
) 