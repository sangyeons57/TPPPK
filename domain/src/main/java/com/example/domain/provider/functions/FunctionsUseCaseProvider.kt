package com.example.domain.provider.functions

import com.example.domain.repository.FunctionsRepository
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.factory.context.FunctionsRepositoryFactoryContext
import com.example.domain.usecase.functions.HelloWorldUseCase
import com.example.domain.usecase.functions.HelloWorldUseCaseImpl
import com.example.domain.usecase.user.UploadProfileImageUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.JvmSuppressWildcards

/**
 * Firebase Functions 관련 UseCase들을 제공하는 Provider
 * Clean Architecture의 UseCase Provider 패턴을 따릅니다.
 */
data class FunctionsUseCases(
    val helloWorldUseCase: HelloWorldUseCase,
    val uploadProfileImageUseCase: UploadProfileImageUseCase,
    val functionsRepository: FunctionsRepository
)

@Singleton
class FunctionsUseCaseProvider @Inject constructor(
    private val functionsRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<FunctionsRepositoryFactoryContext, FunctionsRepository>
) {
    
    /**
     * Firebase Functions 관련 UseCase들을 생성하고 반환합니다.
     * 
     * @return FunctionsUseCases 객체
     */
    fun create(): FunctionsUseCases {
        val functionsRepository = functionsRepositoryFactory.create(FunctionsRepositoryFactoryContext())
        
        return FunctionsUseCases(
            helloWorldUseCase = HelloWorldUseCaseImpl(functionsRepository),
            uploadProfileImageUseCase = UploadProfileImageUseCase(functionsRepository),
            functionsRepository = functionsRepository
        )
    }
}