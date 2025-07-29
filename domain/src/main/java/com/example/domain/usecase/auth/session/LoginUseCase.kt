package com.example.domain.usecase.auth.session

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.repository.local.LocalUserRepository
import com.example.domain.repository.remote.AuthRepository
import javax.inject.Inject

/**
 * 이메일과 비밀번호로 로그인을 시도하고 사용자 세션 정보를 반환하는 UseCase.
 * (기존 LoginUseCase를 수정함)
 *
 * @property authRepository 인증 관련 기능을 제공하는 Repository.
 */
/**
 * Custom exception to indicate that a user account has been withdrawn.
 */
class WithdrawnAccountException(message: String) : Exception(message)

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: LocalUserRepository
) {
    // Centralized TAG for consistent Logcat filtering during debugging
    companion object {
        private const val TAG = "LoginUseCase"
    }

    /**
     * 이메일과 비밀번호를 이용하여 사용자 로그인을 시도합니다.
     *
     * @param credentials EmailPasswordCredentials 객체 (email, password 포함).
     * @return 성공 시 UserSession이 포함된 CustomResult.Success, 실패 시 CustomResult.Error.
     */
    suspend operator fun invoke(
        email: UserEmail,
        password: String
    ): CustomResult<UserSession, Exception> {
        // TODO: Implement LoginUseCase using LocalUserRepository
        // This should:
        // 1. Authenticate user using authRepository.login(email, password)
        // 2. After successful authentication, fetch user details from local storage using userRepository.findById()
        // 3. Check user account status (active/withdrawn) from local data
        // 4. Handle withdrawn accounts by logging out and returning appropriate error
        // 5. Return successful UserSession for active accounts
        // 6. Implement proper error handling with localized messages
        // 7. Use proper logging with TAG for debugging
        // Note: This implementation should work with LocalUserRepository instead of remote server calls
        TODO("LoginUseCase implementation pending - convert to use LocalUserRepository for SSOT pattern")
    }
}