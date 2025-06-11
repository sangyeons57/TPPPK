package com.example.domain.usecase.auth

import com.example.core_common.result.CustomResult
import com.example.domain.repository.AuthRepository
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
    private val authRepository: AuthRepository
) : CheckAuthenticationStatusUseCase {

    /**
     * 유스케이스를 실행하여 사용자의 인증 및 이메일 인증 상태를 확인합니다.
     * @return CustomResult<Pair<Boolean, Boolean>, Exception> (인증 여부, 이메일 인증 여부) 확인 결과
     */
    override suspend fun invoke(): CustomResult<Boolean, Exception> {
        return try {

            if (!authRepository.isLoggedIn())
                return CustomResult.Success(false)

            val result = authRepository.checkEmailVerification()
            when (result){
                is CustomResult.Success<Boolean> -> CustomResult.Success(true)
                else -> CustomResult.Success(false)
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
} 