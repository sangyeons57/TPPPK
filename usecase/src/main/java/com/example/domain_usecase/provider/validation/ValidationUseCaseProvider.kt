package com.example.domain_usecase.provider.validation

import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.UserRepository
import com.example.domain_usecase.usecase.auth.password.ValidateNewPasswordUseCase
import com.example.domain_usecase.usecase.auth.password.ValidatePasswordForSignUpUseCase
import com.example.domain_usecase.usecase.auth.password.ValidatePasswordFormatUseCase
import com.example.domain_usecase.usecase.auth.password.ValidatePasswordResetCodeUseCase
import com.example.domain_usecase.usecase.auth.validation.ValidateEmailForSignUpUseCase
import com.example.domain_usecase.usecase.auth.validation.ValidateEmailFormatUseCase
import com.example.domain_usecase.usecase.auth.validation.ValidateEmailUseCase
import com.example.domain_usecase.usecase.auth.validation.ValidateNicknameForSignUpUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 유효성 검사 관련 UseCase들을 제공하는 Provider
 * 
 * 이메일, 비밀번호, 닉네임 등의 유효성 검사 기능을 담당합니다.
 */
@Singleton
class ValidationUseCaseProvider @Inject constructor(
    private val userRepository: UserRepository
) {

    /**
     * 유효성 검사 관련 UseCase들을 생성합니다.
     * 
     * @return 유효성 검사 관련 UseCase 그룹
     */
    fun create(): ValidationUseCases {
        // Set collection path for user repository
        userRepository.setCollection(CollectionPath.users)

        val validateEmailFormatUseCase = ValidateEmailFormatUseCase()
        
        return ValidationUseCases(
            // 이메일 유효성 검사
            validateEmailFormatUseCase = validateEmailFormatUseCase,
            validateEmailForSignUpUseCase = ValidateEmailForSignUpUseCase(
                validateEmailFormatUseCase = validateEmailFormatUseCase
            ),
            validateEmailUseCase = ValidateEmailUseCase(),
            
            // 비밀번호 유효성 검사
            validatePasswordFormatUseCase = ValidatePasswordFormatUseCase(),
            validatePasswordForSignUpUseCase = ValidatePasswordForSignUpUseCase(),
            validateNewPasswordUseCase = ValidateNewPasswordUseCase(),
            validatePasswordResetCodeUseCase = ValidatePasswordResetCodeUseCase(),
            
            // 닉네임 유효성 검사
            validateNicknameForSignUpUseCase = ValidateNicknameForSignUpUseCase(
                userRepository = this.userRepository
            ),
            
        )
    }
}

/**
 * 유효성 검사 관련 UseCase 그룹
 */
data class ValidationUseCases(
    // 이메일 유효성 검사
    val validateEmailFormatUseCase: ValidateEmailFormatUseCase,
    val validateEmailForSignUpUseCase: ValidateEmailForSignUpUseCase,
    val validateEmailUseCase: ValidateEmailUseCase,
    
    // 비밀번호 유효성 검사
    val validatePasswordFormatUseCase: ValidatePasswordFormatUseCase,
    val validatePasswordForSignUpUseCase: ValidatePasswordForSignUpUseCase,
    val validateNewPasswordUseCase: ValidateNewPasswordUseCase,
    val validatePasswordResetCodeUseCase: ValidatePasswordResetCodeUseCase,
    
    // 닉네임 유효성 검사
    val validateNicknameForSignUpUseCase: ValidateNicknameForSignUpUseCase,

    )