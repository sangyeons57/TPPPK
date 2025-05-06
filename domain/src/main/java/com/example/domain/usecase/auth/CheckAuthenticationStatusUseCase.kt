package com.example.domain.usecase.auth

import com.example.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 사용자의 인증 상태 및 이메일 인증 상태를 확인하는 유스케이스 인터페이스
 */
interface CheckAuthenticationStatusUseCase {
    // (isAuthenticated, isEmailVerified)
    suspend operator fun invoke(): Result<Pair<Boolean, Boolean>>
}

/**
 * CheckAuthenticationStatusUseCase의 구현체
 * @param authRepository 인증 관련 기능을 제공하는 Repository
 */
class CheckAuthenticationStatusUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository
) : CheckAuthenticationStatusUseCase {

    /**
     * 유스케이스를 실행하여 사용자의 인증 및 이메일 인증 상태를 확인합니다.
     * @return Result<Pair<Boolean, Boolean>> (인증 여부, 이메일 인증 여부) 확인 결과
     */
    override suspend fun invoke(): Result<Pair<Boolean, Boolean>> {
        return try {
            val isAuthenticated = authRepository.isLoggedIn()
            
            val isEmailVerified = if (isAuthenticated) {
                authRepository.checkEmailVerification().getOrDefault(false)
            } else {
                false
            }
            
            Result.success(Pair(isAuthenticated, isEmailVerified))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 