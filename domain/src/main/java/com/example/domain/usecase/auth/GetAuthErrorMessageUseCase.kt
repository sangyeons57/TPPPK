package com.example.domain.usecase.auth

import com.example.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 인증 관련 에러 메시지를 가져오는 UseCase
 * 
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class GetAuthErrorMessageUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 로그인 과정에서 발생한 에러 메시지를 가져옵니다.
     *
     * @param exception 발생한 예외
     * @return 사용자에게 표시할 에러 메시지
     */
    suspend fun getLoginErrorMessage(exception: Throwable): String {
        return authRepository.getLoginErrorMessage(exception)
    }

    /**
     * 회원가입 과정에서 발생한 에러 메시지를 가져옵니다.
     *
     * @param exception 발생한 예외
     * @return 사용자에게 표시할 에러 메시지
     */
    suspend fun getSignUpErrorMessage(exception: Throwable): String {
        return authRepository.getSignUpErrorMessage(exception)
    }
    
    /**
     * 비밀번호 재설정 과정에서 발생한 에러 메시지를 가져옵니다.
     *
     * @param exception 발생한 예외
     * @return 사용자에게 표시할 에러 메시지
     */
    suspend fun getPasswordResetErrorMessage(exception: Throwable): String {
        return authRepository.getPasswordResetErrorMessage(exception)
    }
} 