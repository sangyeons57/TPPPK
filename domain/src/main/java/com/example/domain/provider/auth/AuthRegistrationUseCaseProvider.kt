package com.example.domain.provider.auth

import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.example.domain.model.vo.CollectionPath
import com.example.domain.usecase.auth.registration.RequestEmailVerificationAfterSignUpUseCase
import com.example.domain.usecase.auth.registration.SignUpUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 회원가입 및 이메일 인증 UseCase들을 제공하는 Provider
 * 
 * 사용자 등록, 이메일 인증 등의 회원가입 관련 기능을 담당합니다.
 */
@Singleton
class AuthRegistrationUseCaseProvider @Inject constructor(
    private val authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>,
    private val userRepositoryFactory: RepositoryFactory<UserRepositoryFactoryContext, UserRepository>
) {

    /**
     * 회원가입 및 이메일 인증 UseCase들을 생성합니다.
     * 
     * @return 회원가입 관리 UseCase 그룹
     */
    fun create(): AuthRegistrationUseCases {
        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )
        
        val userRepository = userRepositoryFactory.create(
            UserRepositoryFactoryContext(CollectionPath.users)
        )

        return AuthRegistrationUseCases(
            // 회원가입
            signUpUseCase = SignUpUseCase(
                authRepository = authRepository,
                userRepository = userRepository
            ),
            
            // 이메일 인증
            checkEmailVerificationUseCase = CheckEmailVerificationUseCase(
                authRepository = authRepository
            ),
            
            sendEmailVerificationUseCase = SendEmailVerificationUseCase(
                authRepository = authRepository
            ),
            
            requestEmailVerificationAfterSignUpUseCase = RequestEmailVerificationAfterSignUpUseCase(
                authRepository = authRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository,
            userRepository = userRepository
        )
    }
}

/**
 * 회원가입 관리 UseCase 그룹
 */
data class AuthRegistrationUseCases(
    // 회원가입
    val signUpUseCase: SignUpUseCase,
    
    // 이메일 인증
    val checkEmailVerificationUseCase: CheckEmailVerificationUseCase,
    val sendEmailVerificationUseCase: SendEmailVerificationUseCase,
    val requestEmailVerificationAfterSignUpUseCase: RequestEmailVerificationAfterSignUpUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val userRepository: UserRepository
)