package com.example.domain.provider.context

import android.content.Context
import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.example.domain.usecase.user.UploadProfileImageUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider for UseCases that require Android Context
 * 
 * Some UseCases need Android context which can't be provided through 
 * the standard repository factory pattern alone.
 */
@Singleton
class ContextDependentUseCaseProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<UserRepositoryFactoryContext, UserRepository>,
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * Creates context-dependent UseCases
     * 
     * @return ContextDependentUseCases containing all context-dependent use cases
     */
    fun create(): ContextDependentUseCases {
        val userRepository = userRepositoryFactory.create(
            UserRepositoryFactoryContext(CollectionPath.users)
        )
        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )
        
        return ContextDependentUseCases(
            uploadProfileImageUseCase = UploadProfileImageUseCase(
                userRepository = userRepository,
                authRepository = authRepository,
                context = context
            ),
            
            // Repository references for potential future use
            userRepository = userRepository,
            authRepository = authRepository
        )
    }
}

/**
 * Context-dependent UseCase group
 */
data class ContextDependentUseCases(
    val uploadProfileImageUseCase: UploadProfileImageUseCase,
    
    // Repository references
    val userRepository: UserRepository,
    val authRepository: AuthRepository
)