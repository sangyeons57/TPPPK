package com.example.domain_usecase.provider.dev

import com.example.domain.vo.CollectionPath
import com.example.domain_repository.RepositoryFactory
import com.example.domain_repository.base.UserRepository
import com.example.domain_repository.context.UserRepositoryFactoryContext
import com.example.domain_usecase.usecase.dev.SendFcmTestNotificationUseCase
import com.example.domain_usecase.usecase.dev.SendFcmTestNotificationUseCaseImpl
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