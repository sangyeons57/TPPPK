package com.example.domain.usecase.auth

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.usecase.user.GetUserUseCase
import kotlinx.coroutines.flow.first
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
    private val getUserUseCase: GetUserUseCase,
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
    suspend operator fun invoke(email : String, password : String): CustomResult<UserSession, Exception> {
        // 시작 로그 – 입력된 이메일 기준으로 로그인 시도
        Log.d(TAG, "Attempting login for email=$email")
        return when (val loginResult = authRepository.login(email, password)) {
            is CustomResult.Success -> {
                val userSession = loginResult.data
                // AuthRepository 에서 세션 획득 성공
                Log.d(TAG, "Auth success. Session=$userSession")
                // After successful authentication, fetch user details to check account status
                Log.d(TAG, "Fetching user details (may hit cache) for userId=${userSession.userId}")
                when (val userDetailsResult = getUserUseCase(userSession.userId).first()) {
                    is CustomResult.Success -> {
                        val user = userDetailsResult.data
                        // Firestore 에서 가져온 사용자 정보 (캐시 또는 서버)
                        Log.d(TAG, "Fetched User (possibly from cache): $user")
                        Log.d(TAG, "Account Status: ${user}")
                        if (user.accountStatus == UserAccountStatus.WITHDRAWN) {
                            Log.d(TAG, "Cached status=WITHDRAWN, verifying against server...")
                            when (val userResult = userRepository.observe(DocumentId(userSession.userId)).first()) {
                                is CustomResult.Success -> {
                                    val remoteUser = userResult.data
                                    Log.d(TAG, "Server status=${remoteUser.accountStatus}")
                                    if (remoteUser.accountStatus == UserAccountStatus.WITHDRAWN) {
                                        authRepository.logout()
                                        CustomResult.Failure(WithdrawnAccountException("탈퇴한 계정입니다. (서버 확인)"))
                                    } else {
                                        // Remote says ACTIVE -> stale cache, continue login success
                                        loginResult
                                    }
                                }
                                is CustomResult.Failure -> {
                                    authRepository.logout()
                                    CustomResult.Failure(userResult.error)
                                }
                                else -> {
                                    authRepository.logout()
                                    CustomResult.Failure(Exception("알 수 없는 오류 (원격 사용자 확인)"))
                                }
                            }
                        } else {
                            // Account is active, return the original success result with session
                            loginResult
                        }
                    }
                    is CustomResult.Failure -> {
                        // Failed to fetch user details – sign out to avoid stale session and propagate failure
                        authRepository.logout()
                        CustomResult.Failure(userDetailsResult.error ?: Exception("사용자 정보를 가져오는데 실패했습니다."))
                    }
                    else -> {
                        Log.d(TAG, "Unexpected userDetailsResult state: $userDetailsResult")
                        CustomResult.Failure(Exception("알 수 없는 오류가 발생했습니다."))
                    }
                }
            }
            is CustomResult.Failure -> {
                // Login authentication failed, log failure reason
                Log.d(TAG, "Authentication failed: ${loginResult.error}")
                // Login authentication failed, return the original failure
                loginResult
            }
            else -> {
                 CustomResult.Failure(Exception("알 수 없는 로그인 오류가 발생했습니다."))
            }
        }
    }
}