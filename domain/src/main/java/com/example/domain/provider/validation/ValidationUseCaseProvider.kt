package com.example.domain.provider.validation

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.example.domain.usecase.auth.validation.ValidateEmailFormatUseCase
import com.example.domain.usecase.auth.validation.ValidateEmailForSignUpUseCase
import com.example.domain.usecase.auth.validation.ValidateEmailUseCase
import com.example.domain.usecase.auth.password.ValidateNewPasswordUseCase
import com.example.domain.usecase.auth.validation.ValidateNicknameForSignUpUseCase
import com.example.domain.usecase.auth.password.ValidatePasswordFormatUseCase
import com.example.domain.usecase.auth.password.ValidatePasswordForSignUpUseCase
import com.example.domain.usecase.auth.password.ValidatePasswordResetCodeUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 유효성 검사 관련 UseCase들을 제공하는 Provider
 * 
 * 이메일, 비밀번호, 닉네임 등의 유효성 검사 기능을 담당합니다.
 */
@Singleton
class ValidationUseCaseProvider @Inject constructor(
    private val userRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<UserRepositoryFactoryContext, UserRepository>
) {

    /**
     * 유효성 검사 관련 UseCase들을 생성합니다.
     * 
     * @return 유효성 검사 관련 UseCase 그룹
     */
    fun create(): ValidationUseCases {
        val userRepository = userRepositoryFactory.create(
            UserRepositoryFactoryContext(
                collectionPath = CollectionPath.users
            )
        )

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
                userRepository = userRepository
            ),
            
            // 공통 Repository
            userRepository = userRepository
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
    
    // 공통 Repository
    val userRepository: UserRepository
)