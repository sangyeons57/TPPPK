package com.example.domain.usecase.auth

import com.example.domain.model.User
import com.example.domain.repository.AuthRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자 로그인 기능을 수행하는 UseCase
 * 
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 * @property checkEmailVerificationUseCase 이메일 인증 상태를 확인하는 UseCase
 * @property sendEmailVerificationUseCase 이메일 인증 메일을 발송하는 UseCase
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val checkEmailVerificationUseCase: CheckEmailVerificationUseCase,
    private val sendEmailVerificationUseCase: SendEmailVerificationUseCase
) {
    /**
     * 이메일과 비밀번호를 이용하여 사용자 로그인을 수행합니다.
     * 로그인 성공 후에는 이메일 인증 여부를 확인하고, 인증되지 않았으면 이메일 인증 메일을 발송하고 실패를 반환합니다.
     *
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 성공 시 사용자 정보가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(email: String, password: String): Result<User?> {
        val loginResult = authRepository.login(email, password)
        
        return loginResult.fold(
            onSuccess = { user ->
                // 로그인 성공 후 이메일 인증 상태 확인
                val emailVerificationResult = checkEmailVerificationUseCase()
                
                emailVerificationResult.fold(
                    onSuccess = { isVerified ->
                        if (isVerified) {
                            // 이메일 인증이 완료된 상태이면 로그인 성공 처리
                            Result.success(user)
                        } else {
                            // 이메일 인증이 되지 않은 상태이면 인증 메일 발송 후 실패 처리
                            sendEmailVerificationUseCase()
                            Result.failure(EmailNotVerifiedException("이메일 인증이 완료되지 않았습니다. 인증 메일을 확인해주세요."))
                        }
                    },
                    onFailure = { 
                        // 이메일 인증 상태 확인에 실패했을 경우 로그인 실패 처리
                        Result.failure(it)
                    }
                )
            },
            onFailure = { 
                // 로그인 자체가 실패한 경우
                Result.failure(it)
            }
        )
    }
    
    /**
     * 이메일 인증이 완료되지 않았을 때 발생하는 예외
     */
    class EmailNotVerifiedException(message: String): Exception(message)
} 