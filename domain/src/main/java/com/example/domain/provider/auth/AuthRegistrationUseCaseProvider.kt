package com.example.domain.provider.auth

import com.example.domain.model.base.User
import com.example.domain.repository.remote.AuthRepository
import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.remote.DefaultRepository
import com.example.domain.usecase.auth.CheckEmailVerificationUseCase
import com.example.domain.usecase.auth.SendEmailVerificationUseCase
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
    private val authRepository: AuthRepository,
    private val userRepository: DefaultRepository<User>
) {

    /**
     * 회원가입 및 이메일 인증 UseCase들을 생성합니다.
     * 
     * @return 회원가입 관리 UseCase 그룹
     */
    fun create(): AuthRegistrationUseCases {
        userRepository.setCollection(CollectionPath.users)

        return AuthRegistrationUseCases(
            authRepository = authRepository,
            userRepository = userRepository,

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
    val authRepository: AuthRepository,
    val userRepository: DefaultRepository<User>
)