package com.example.domain.provider.auth

import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.example.domain.usecase.auth.DeleteAuthUserUseCase
import com.example.domain.usecase.auth.account.ReactivateAccountUseCase
import com.example.domain.usecase.auth.account.WithdrawMembershipUseCase
import com.example.domain.usecase.auth.account.WithdrawMembershipUseCaseImpl
import com.example.domain.model.vo.CollectionPath
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 계정 관리 UseCase들을 제공하는 Provider
 * 
 * 계정 삭제, 재활성화, 탈퇴 등의 계정 관리 기능을 담당합니다.
 */
@Singleton
class AuthAccountUseCaseProvider @Inject constructor(
    private val authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>,
    private val userRepositoryFactory: RepositoryFactory<UserRepositoryFactoryContext, UserRepository>
) {

    /**
     * 계정 관리 UseCase들을 생성합니다.
     * 
     * @return 계정 관리 UseCase 그룹
     */
    fun create(): AuthAccountUseCases {
        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )
        
        val userRepository = userRepositoryFactory.create(
            UserRepositoryFactoryContext(CollectionPath.users)
        )

        return AuthAccountUseCases(
            // 계정 관리
            deleteAuthUserUseCase = DeleteAuthUserUseCase(
                authRepository = authRepository
            ),
            
            reactivateAccountUseCase = ReactivateAccountUseCase(
                authRepository = authRepository,
                userRepository = userRepository
            ),
            
            withdrawMembershipUseCase = WithdrawMembershipUseCaseImpl(
                authRepository = authRepository,
                userRepository = userRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository,
            userRepository = userRepository
        )
    }
}

/**
 * 계정 관리 UseCase 그룹
 */
data class AuthAccountUseCases(
    // 계정 관리
    val deleteAuthUserUseCase: DeleteAuthUserUseCase,
    val reactivateAccountUseCase: ReactivateAccountUseCase,
    val withdrawMembershipUseCase: WithdrawMembershipUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val userRepository: UserRepository
)