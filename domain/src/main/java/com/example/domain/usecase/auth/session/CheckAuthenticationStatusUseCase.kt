package com.example.domain.usecase.auth.session

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.LocalUserRepository
import com.example.domain.repository.remote.AuthRepository
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
    private val userRepository: LocalUserRepository,
    private val authRepository: AuthRepository
) : CheckAuthenticationStatusUseCase {

    /**
     * 유스케이스를 실행하여 사용자의 인증 및 이메일 인증 상태를 확인합니다.
     * @return CustomResult<Boolean, Exception> 로그인되고 이메일 인증이 완료된 경우 true
     */
    override suspend fun invoke(): CustomResult<Boolean, Exception> {
        // TODO: Implement CheckAuthenticationStatusUseCase using LocalUserRepository
        // This should:
        // 1. Check if user is logged in using authRepository.isLoggedIn()
        // 2. Return Success(false) if not logged in
        // 3. Check email verification status using authRepository.checkEmailVerification()
        // 4. Handle email verification CustomResult states:
        //    - Success: return the email verification status
        //    - Failure: distinguish between network/timeout vs authentication errors
        //    - For network issues: return Success(false) for safety
        //    - For auth errors: propagate the failure
        //    - Other states: return Success(false)
        // 5. Wrap entire operation in try-catch for exception handling
        // Note: This should work with LocalUserRepository for consistent data access
        TODO("CheckAuthenticationStatusUseCase implementation pending - convert to use LocalUserRepository for SSOT pattern")
    }
} 