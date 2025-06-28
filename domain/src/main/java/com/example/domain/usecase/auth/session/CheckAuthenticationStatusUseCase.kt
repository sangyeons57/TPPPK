package com.example.domain.usecase.auth

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import javax.inject.Inject

/**
 * 사용자의 인증 상태 및 이메일 인증 상태를 확인하는 유스케이스 인터페이스
 */
interface CheckAuthenticationStatusUseCase {
    // (isAuthenticated, isEmailVerified)
    suspend operator fun invoke(): CustomResult<Boolean, Exception>
}

/**
 * CheckAuthenticationStatusUseCase의 구현체
 * @param authRepository 인증 관련 기능을 제공하는 Repository
 */
class CheckAuthenticationStatusUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : CheckAuthenticationStatusUseCase {

    /**
     * 유스케이스를 실행하여 사용자의 인증 및 이메일 인증 상태를 확인합니다.
     * @return CustomResult<Boolean, Exception> 로그인되고 이메일 인증이 완료된 경우 true
     */
    override suspend fun invoke(): CustomResult<Boolean, Exception> {
        return try {
            // Step 1: Check if user is logged in
            if (!authRepository.isLoggedIn()) {
                return CustomResult.Success(false)
            }

            // Step 2: Check email verification status
            val emailVerificationResult = authRepository.checkEmailVerification()
            when (emailVerificationResult) {
                is CustomResult.Success -> {
                    // Return true only if email is verified
                    CustomResult.Success(emailVerificationResult.data)
                }
                is CustomResult.Failure -> {
                    // Handle email verification check failure
                    // Check if it's a network/timeout issue vs authentication issue
                    val errorMessage = emailVerificationResult.error.message ?: ""
                    
                    if (errorMessage.contains("timed out", ignoreCase = true) || 
                        errorMessage.contains("network", ignoreCase = true)) {
                        // For network issues, try to proceed with cached status
                        // but mark as not fully verified for safety
                        CustomResult.Success(false)
                    } else {
                        // For authentication errors, the user should re-login
                        CustomResult.Failure(emailVerificationResult.error)
                    }
                }
                else -> {
                    CustomResult.Success(false)
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
} 