package com.example.domain.provider.functions

import com.example.domain.repository.base.UserRepository
import com.example.domain.usecase.functions.HelloWorldUseCase
import com.example.domain.usecase.functions.HelloWorldUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.JvmSuppressWildcards

/**
 * Firebase Functions 관련 UseCase들을 제공하는 Provider
 * Clean Architecture의 UseCase Provider 패턴을 따릅니다.
 */
data class FunctionsUseCases(
    val helloWorldUseCase: HelloWorldUseCase,
    val userRepository: UserRepository
)

@Singleton
class FunctionsUseCaseProvider @Inject constructor(
    private val userRepository: UserRepository
) {
    
    /**
     * Firebase Functions 관련 UseCase들을 생성하고 반환합니다.
     * 
     * @return FunctionsUseCases 객체
     */
    fun create(): FunctionsUseCases {
        return FunctionsUseCases(
            helloWorldUseCase = HelloWorldUseCaseImpl(userRepository),
            userRepository = userRepository
        )
    }
}