package com.example.domain_usecase.provider.auth

import com.example.domain_repository.base.AuthRepository
import com.example.domain_usecase.usecase.auth.password.RequestPasswordResetUseCase
import com.example.domain_usecase.usecase.auth.password.ValidateNewPasswordUseCase
import com.example.domain_usecase.usecase.auth.password.ValidatePasswordForSignUpUseCase
import com.example.domain_usecase.usecase.auth.password.ValidatePasswordFormatUseCase
import com.example.domain_usecase.usecase.auth.password.ValidatePasswordResetCodeUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 비밀번호 관리 UseCase들을 제공하는 Provider
 * 
 * 비밀번호 재설정, 유효성 검사 등의 비밀번호 관련 기능을 담당합니다.
 */
@Singleton
class AuthPasswordUseCaseProvider @Inject constructor(
    private val authRepository: AuthRepository
) {

    /**
     * 비밀번호 관리 UseCase들을 생성합니다.
     * 
     * @return 비밀번호 관리 UseCase 그룹
     */
    fun create(): AuthPasswordUseCases {

        return AuthPasswordUseCases(
            // 비밀번호 재설정
            requestPasswordResetUseCase = RequestPasswordResetUseCase(
                authRepository = this.authRepository
            ),
            
            validatePasswordResetCodeUseCase = ValidatePasswordResetCodeUseCase(),
            
            // 비밀번호 유효성 검사
            validateNewPasswordUseCase = ValidateNewPasswordUseCase(),
            
            validatePasswordFormatUseCase = ValidatePasswordFormatUseCase(),
            
            validatePasswordForSignUpUseCase = ValidatePasswordForSignUpUseCase()
        )
    }
}

/**
 * 비밀번호 관리 UseCase 그룹
 */
data class AuthPasswordUseCases(
    // 비밀번호 재설정
    val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    val validatePasswordResetCodeUseCase: ValidatePasswordResetCodeUseCase,
    
    // 비밀번호 유효성 검사
    val validateNewPasswordUseCase: ValidateNewPasswordUseCase,
    val validatePasswordFormatUseCase: ValidatePasswordFormatUseCase,
    val validatePasswordForSignUpUseCase: ValidatePasswordForSignUpUseCase
)