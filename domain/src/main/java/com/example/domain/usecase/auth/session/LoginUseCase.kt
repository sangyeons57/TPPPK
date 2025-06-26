package com.example.domain.usecase.auth.session

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.data.UserSession
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Email
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
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
    private val userRepository: UserRepository
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
    suspend operator fun invoke(email : Email, password : String): CustomResult<UserSession, Exception> {
        // 시작 로그 – 입력된 이메일 기준으로 로그인 시도
        //(TAG, "Attempting login for email=$email")
        return when (val loginResult = authRepository.login(email, password)) {
            is CustomResult.Success -> {
                val userSession = loginResult.data
                // AuthRepository 에서 세션 획득 성공
                //(TAG, "Auth success. Session=$userSession")
                // After successful authentication, fetch user details to check account status
                //(TAG, "Fetching user details (may hit cache) for userId=${userSession.userId}")
                when (val userResult = userRepository.findById(DocumentId.from(userSession.userId))) {
                    is CustomResult.Success -> {
                        // Firestore 에서 가져온 사용자 정보 (캐시 또는 서버)
                        val user = userResult.data as User
                        //(TAG, "Fetched User (possibly from cache): ${user}")
                        if (user.accountStatus == UserAccountStatus.WITHDRAWN) {
                            //(TAG, "Cached status=WITHDRAWN, verifying against server...")
                            authRepository.logout()
                            CustomResult.Failure(WithdrawnAccountException("탈퇴한 계정입니다. (서버 확인)"))
                        } else {
                            // Account is active, return the original success result with session
                            loginResult
                        }
                    }
                    is CustomResult.Failure -> {
                        // Failed to fetch user details – sign out to avoid stale session and propagate failure
                        authRepository.logout()
                        CustomResult.Failure(userResult.error)
                    }
                    else -> {
                        //(TAG, "Unexpected userDetailsResult state: $userResult")
                        CustomResult.Failure(Exception("알 수 없는 오류가 발생했습니다."))
                    }
                }
            }
            is CustomResult.Failure -> {
                // Login authentication failed, log failure reason
                //(TAG, "Authentication failed: ${loginResult.error}")
                // Login authentication failed, return the original failure
                loginResult
            }
            else -> {
                 CustomResult.Failure(Exception("알 수 없는 로그인 오류가 발생했습니다."))
            }
        }
    }
}