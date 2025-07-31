package com.example.domain_usecase.provider.auth

import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.UserRepository
import com.example.domain_usecase.usecase.auth.validation.GetAuthErrorMessageUseCase
import com.example.domain_usecase.usecase.auth.validation.GetAuthErrorMessageUseCaseImpl
import com.example.domain_usecase.usecase.auth.validation.ValidateEmailForSignUpUseCase
import com.example.domain_usecase.usecase.auth.validation.ValidateEmailFormatUseCase
import com.example.domain_usecase.usecase.auth.validation.ValidateEmailUseCase
import com.example.domain_usecase.usecase.auth.validation.ValidateNicknameForSignUpUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 유효성 검사 UseCase들을 제공하는 Provider
 * 
 * 이메일, 닉네임 등의 입력값 유효성 검사 및 오류 메시지 처리를 담당합니다.
 */
@Singleton
class AuthValidationUseCaseProvider @Inject constructor(
    private val userRepository: UserRepository
) {

    /**
     * 유효성 검사 UseCase들을 생성합니다.
     * 
     * @return 유효성 검사 UseCase 그룹
     */
    fun create(): AuthValidationUseCases {
        // Set collection path for user repository
        userRepository.setCollection(CollectionPath.users)

        val validateEmailFormatUseCase = ValidateEmailFormatUseCase()
        
        return AuthValidationUseCases(
            // 이메일 유효성 검사
            validateEmailUseCase = ValidateEmailUseCase(),
            
            validateEmailFormatUseCase = validateEmailFormatUseCase,
            
            validateEmailForSignUpUseCase = ValidateEmailForSignUpUseCase(
                validateEmailFormatUseCase = validateEmailFormatUseCase
            ),
            
            // 닉네임 유효성 검사
            validateNicknameForSignUpUseCase = ValidateNicknameForSignUpUseCase(
                userRepository = this.userRepository
            ),

            // 오류 메시지 처리
            getAuthErrorMessageUseCase = GetAuthErrorMessageUseCaseImpl()
        )
    }
}

/**
 * 유효성 검사 UseCase 그룹
 */
data class AuthValidationUseCases(
    // 이메일 유효성 검사
    val validateEmailUseCase: ValidateEmailUseCase,
    val validateEmailFormatUseCase: ValidateEmailFormatUseCase,
    val validateEmailForSignUpUseCase: ValidateEmailForSignUpUseCase,
    
    // 닉네임 유효성 검사
    val validateNicknameForSignUpUseCase: ValidateNicknameForSignUpUseCase,

    // 오류 메시지 처리
    val getAuthErrorMessageUseCase: GetAuthErrorMessageUseCase
)