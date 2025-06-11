package com.example.domain.usecase.auth

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.repository.AuthRepository
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
    private val getUserUseCase: GetUserUseCase
) {
    /**
     * 이메일과 비밀번호를 이용하여 사용자 로그인을 시도합니다.
     *
     * @param credentials EmailPasswordCredentials 객체 (email, password 포함).
     * @return 성공 시 UserSession이 포함된 CustomResult.Success, 실패 시 CustomResult.Error.
     */
    suspend operator fun invoke(email : String, password : String): CustomResult<UserSession, Exception> {
        return when (val loginResult = authRepository.login(email, password)) {
            is CustomResult.Success -> {
                val userSession = loginResult.data
                // After successful authentication, fetch user details to check account status
                Log.d("LoginUseCase", userSession.toString())
                when (val userDetailsResult = getUserUseCase(userSession.userId).first()) {
                    is CustomResult.Success -> {
                        val user = userDetailsResult.data
                        Log.d("LoginUseCase", "Account Status: ${user}")
                        if (user.accountStatus == UserAccountStatus.WITHDRAWN) {
                            // If account is withdrawn, sign out the session to prevent auto-login and return failure
                            authRepository.logout()
                            CustomResult.Failure(WithdrawnAccountException("탈퇴한 계정입니다."))
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
                        CustomResult.Failure(Exception("알 수 없는 오류가 발생했습니다."))
                    }
                }
            }
            is CustomResult.Failure -> {
                // Login authentication failed, return the original failure
                loginResult
            }
            else -> {
                 CustomResult.Failure(Exception("알 수 없는 로그인 오류가 발생했습니다."))
            }
        }
    }
}