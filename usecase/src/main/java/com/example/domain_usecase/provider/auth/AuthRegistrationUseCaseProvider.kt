package com.example.domain_usecase.provider.auth

import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.UserRepository
import com.example.domain_usecase.usecase.auth.registration.CheckEmailVerificationUseCase
import com.example.domain_usecase.usecase.auth.registration.RequestEmailVerificationAfterSignUpUseCase
import com.example.domain_usecase.usecase.auth.registration.SendEmailVerificationUseCase
import com.example.domain_usecase.usecase.auth.registration.SignUpUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 회원가입 및 이메일 인증 UseCase들을 제공하는 Provider
 * 
 * 사용자 등록, 이메일 인증 등의 회원가입 관련 기능을 담당합니다.
 */
@Singleton
class AuthRegistrationUseCaseProvider @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {

    /**
     * 회원가입 및 이메일 인증 UseCase들을 생성합니다.
     * 
     * @return 회원가입 관리 UseCase 그룹
     */
    fun create(): AuthRegistrationUseCases {
        // Set collection path for user repository
        userRepository.setCollection(CollectionPath.users)

        return AuthRegistrationUseCases(
            // 회원가입
            signUpUseCase = SignUpUseCase(
                authRepository = this.authRepository,
                userRepository = this.userRepository
            ),
            
            // 이메일 인증
            checkEmailVerificationUseCase = CheckEmailVerificationUseCase(
                authRepository = this.authRepository
            ),
            
            sendEmailVerificationUseCase = SendEmailVerificationUseCase(
                authRepository = this.authRepository
            ),
            
            requestEmailVerificationAfterSignUpUseCase = RequestEmailVerificationAfterSignUpUseCase(
                authRepository = this.authRepository
            )
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
    val requestEmailVerificationAfterSignUpUseCase: RequestEmailVerificationAfterSignUpUseCase
)