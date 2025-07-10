package com.example.domain.usecase.auth.session

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.data.UserSession
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import com.google.firebase.firestore.Source
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
    suspend operator fun invoke(
        email: UserEmail,
        password: String
    ): CustomResult<UserSession, Exception> {
        // 시작 로그 – 입력된 이메일 기준으로 로그인 시도
        android.util.Log.d(TAG, "Attempting login for email=${email.value}")
        return when (val loginResult = authRepository.login(email, password)) {
            is CustomResult.Success -> {
                val userSession = loginResult.data
                // AuthRepository 에서 세션 획득 성공
                android.util.Log.d(TAG, "Auth success. Session=${userSession}")
                // After successful authentication, fetch user details to check account status
                android.util.Log.d(TAG, "Fetching user details from server for userId=${userSession.userId}")
                when (val userResult = userRepository.findById(DocumentId.from(userSession.userId), Source.SERVER)) {
                    is CustomResult.Success -> {
                        // Firestore 에서 가져온 사용자 정보 (서버)
                        val user = userResult.data as User
                        android.util.Log.d(TAG, "Fetched User from server: accountStatus=${user.accountStatus}")
                        if (user.accountStatus == UserAccountStatus.WITHDRAWN) {
                            android.util.Log.w(TAG, "Account is WITHDRAWN, logging out completely")
                            authRepository.logoutCompletely()
                            CustomResult.Failure(WithdrawnAccountException("탈퇴한 계정입니다. (서버 확인)"))
                        } else {
                            // Account is active, return the original success result with session
                            android.util.Log.d(TAG, "Account is active, login successful")
                            loginResult
                        }
                    }
                    is CustomResult.Failure -> {
                        // Failed to fetch user details – sign out to avoid stale session and propagate failure
                        val errorMessage = when {
                            userResult.error.message?.contains("client has already been terminated", ignoreCase = true) == true -> 
                                "로그인 처리 중 시스템 상태가 변경되었습니다. 잠시 후 다시 시도해주세요."
                            userResult.error.message?.contains("network", ignoreCase = true) == true -> 
                                "네트워크 연결을 확인해주세요."
                            userResult.error.message?.contains("timeout", ignoreCase = true) == true -> 
                                "서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요."
                            userResult.error.message?.contains("permission", ignoreCase = true) == true -> 
                                "사용자 정보 접근 권한이 없습니다. 관리자에게 문의하세요."
                            else -> 
                                "사용자 정보를 가져오는 중 오류가 발생했습니다. 다시 시도해주세요."
                        }
                        android.util.Log.e(TAG, "Failed to fetch user details after login: ${userResult.error.message}", userResult.error)
                        authRepository.logoutCompletely()
                        CustomResult.Failure(Exception(errorMessage))
                    }
                    else -> {
                        android.util.Log.e(TAG, "Unexpected userDetailsResult state: $userResult")
                        authRepository.logoutCompletely()
                        CustomResult.Failure(Exception("사용자 정보 조회 중 예상치 못한 오류가 발생했습니다."))
                    }
                }
            }
            is CustomResult.Failure -> {
                // Login authentication failed, log failure reason
                android.util.Log.e(TAG, "Authentication failed: ${loginResult.error.message}", loginResult.error)
                // Login authentication failed, return the original failure
                loginResult
            }
            else -> {
                android.util.Log.e(TAG, "Unexpected login result state: $loginResult")
                CustomResult.Failure(Exception("로그인 처리 중 예상치 못한 오류가 발생했습니다."))
            }
        }
    }
}