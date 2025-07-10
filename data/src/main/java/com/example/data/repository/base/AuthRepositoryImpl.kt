package com.example.data.repository.base

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Initial.getOrThrow
import com.example.data.datasource.remote.special.AuthRemoteDataSource
import com.example.data.service.CacheService
import com.example.data.util.FirebaseAuthWrapper
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.Token
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.base.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Firebase Authentication을 사용하여 AuthRepository 인터페이스를 구현한 클래스
 * 인증 관련 작업과 세션 관리를 담당합니다.
 */
class AuthRepositoryImpl @Inject constructor(
    private val authWrapper: FirebaseAuthWrapper, // 추가: FirebaseAuthWrapper 주입
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val cacheService: CacheService, // 캐시 정리를 위한 서비스
): AuthRepository {


    /**
     * 이메일과 비밀번호로 로그인합니다.
     * 성공 시 UserSession 객체를 반환합니다.
     * 
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 성공 시 UserSession이 포함된 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun login(
        email: UserEmail,
        password: String
    ): CustomResult<UserSession, Exception> {
        Log.d("AuthRepositoryImpl", "Starting login process for email: ${email.value}")
        
        return when (val loginResult = authRemoteDataSource.signIn(email, password)) {
            is CustomResult.Success -> {
                Log.d("AuthRepositoryImpl", "Firebase Auth login successful, userId: ${loginResult.data}")
                
                // 로그인 성공 시 세션 정보 가져오기 (재시도 로직 포함)
                var retryCount = 0
                val maxRetries = 3
                var lastError: Exception? = null
                
                while (retryCount <= maxRetries) {
                    when (val userSessionResult = getCurrentUserSession()) {
                        is CustomResult.Success -> {
                            Log.d("AuthRepositoryImpl", "Session retrieved successfully after ${retryCount} retries")
                            return CustomResult.Success(userSessionResult.data)
                        }
                        is CustomResult.Failure -> {
                            lastError = userSessionResult.error
                            Log.w("AuthRepositoryImpl", "Failed to get session (attempt ${retryCount + 1}/${maxRetries + 1}): ${lastError?.message}")
                            
                            if (retryCount < maxRetries) {
                                // 네트워크 오류나 일시적 오류인 경우 재시도
                                if (lastError?.message?.contains("network", ignoreCase = true) == true ||
                                    lastError?.message?.contains("timeout", ignoreCase = true) == true) {
                                    kotlinx.coroutines.delay(1000L * (retryCount + 1)) // 지수 백오프
                                    retryCount++
                                    continue
                                } else {
                                    // 재시도 불가능한 오류
                                    break
                                }
                            }
                            retryCount++
                        }
                        else -> {
                            Log.e("AuthRepositoryImpl", "Unexpected session result type: $userSessionResult")
                            lastError = Exception("세션 정보 조회 중 예상치 못한 오류가 발생했습니다.")
                            break
                        }
                    }
                }
                
                // 모든 재시도 실패
                Log.e("AuthRepositoryImpl", "Failed to get session after $maxRetries retries, last error: ${lastError?.message}")
                CustomResult.Failure(lastError ?: Exception("로그인 후 세션 정보를 가져오는 데 실패했습니다."))
            }
            is CustomResult.Failure -> {
                Log.e("AuthRepositoryImpl", "Firebase Auth login failed: ${loginResult.error.message}", loginResult.error)
                CustomResult.Failure(loginResult.error)
            }
            else -> {
                Log.e("AuthRepositoryImpl", "Unexpected login result type: $loginResult")
                CustomResult.Failure(Exception("로그인 처리 중 예상치 못한 오류가 발생했습니다."))
            }
        }
    }
    /**
     * 로그인 상태를 확인합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * @return 로그인 상태 (true: 로그인됨, false: 로그아웃됨)
     */
    override suspend fun isLoggedIn(): Boolean {
        return authWrapper.getCurrentUser() != null
    }

    /**
     * 로그아웃합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * @return 성공 시 Result.success(Unit), 실패 시 Result.failure
     */
    override suspend fun logout(): CustomResult<Unit, Exception> {
        return authRemoteDataSource.signOut()
    }

    /**
     * 완전 로그아웃합니다.
     * Firebase Auth 로그아웃 + 모든 캐시 정리를 수행합니다.
     * 권한 문제나 인증 만료로 인한 로그아웃 시 사용하면 좋습니다.
     * @return 성공 시 Result.success(Unit), 실패 시 Result.failure
     */
    override suspend fun logoutCompletely(): CustomResult<Unit, Exception> {
        Log.d("AuthRepositoryImpl", "Starting complete logout process")
        return try {
            // 1. Firebase Auth 로그아웃
            val authLogoutResult = authRemoteDataSource.signOut()
            
            when (authLogoutResult) {
                is CustomResult.Success -> {
                    Log.d("AuthRepositoryImpl", "Firebase Auth logout successful")
                    
                    // Firebase Auth 상태 변경 대기 (최대 3초)
                    val authStateCleared = waitForAuthStateChange(maxWaitTimeMs = 3000)
                    if (!authStateCleared) {
                        Log.w("AuthRepositoryImpl", "Auth state did not clear within timeout, continuing with cache clearing")
                    }
                    
                    // 2. 모든 캐시 정리
                    when (val cacheResult = cacheService.clearAllCache()) {
                        is CustomResult.Success -> {
                            Log.d("AuthRepositoryImpl", "Cache cleared successfully")
                        }
                        is CustomResult.Failure -> {
                            Log.w("AuthRepositoryImpl", "Auth logout successful but cache clearing failed", cacheResult.error)
                        }
                        else -> {
                            Log.w("AuthRepositoryImpl", "Unexpected cache clearing result: $cacheResult")
                        }
                    }
                    
                    // 3. 로그아웃 완료 검증
                    kotlinx.coroutines.delay(500) // Auth 상태 변경 전파를 위한 짧은 대기
                    val isLogoutVerified = verifyLogoutComplete()
                    
                    if (isLogoutVerified) {
                        Log.d("AuthRepositoryImpl", "Complete logout successful: Auth + Cache cleared and verified")
                        CustomResult.Success(Unit)
                    } else {
                        Log.w("AuthRepositoryImpl", "Logout verification failed but auth logout was successful")
                        // Auth 로그아웃은 성공했으므로 Success로 반환하되 경고 로그 남김
                        CustomResult.Success(Unit)
                    }
                }
                is CustomResult.Failure -> {
                    Log.e("AuthRepositoryImpl", "Auth logout failed during complete logout", authLogoutResult.error)
                    authLogoutResult
                }
                else -> {
                    Log.e("AuthRepositoryImpl", "Unexpected auth logout result: $authLogoutResult")
                    CustomResult.Failure(Exception("로그아웃 처리 중 예상치 못한 오류가 발생했습니다."))
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Exception during complete logout", e)
            CustomResult.Failure(e)
        }
    }
    
    /**
     * Firebase Auth 상태 변경을 대기합니다.
     * 로그아웃 후 Auth 상태가 완전히 정리되었는지 확인합니다.
     * @param maxWaitTimeMs 최대 대기 시간 (밀리초)
     * @return Auth 상태가 정리되었으면 true, 타임아웃이면 false
     */
    private suspend fun waitForAuthStateChange(maxWaitTimeMs: Long): Boolean {
        return withTimeoutOrNull(maxWaitTimeMs) {
            var attempts = 0
            val maxAttempts = (maxWaitTimeMs / 100).toInt()
            
            // Auth 상태가 null이 될 때까지 대기
            while (authWrapper.getCurrentUser() != null && attempts < maxAttempts) {
                attempts++
                Log.d("AuthRepositoryImpl", "Waiting for auth state to clear... attempt $attempts/$maxAttempts")
                kotlinx.coroutines.delay(100)
            }
            
            val isCleared = authWrapper.getCurrentUser() == null
            Log.d("AuthRepositoryImpl", "Auth state cleared: $isCleared after $attempts attempts")
            isCleared
        } ?: false
    }
    
    /**
     * 로그아웃 후 Firebase Auth 상태를 추가로 검증합니다.
     * @return Firebase Auth가 완전히 로그아웃되었으면 true
     */
    private suspend fun verifyLogoutComplete(): Boolean {
        return try {
            // 1. AuthWrapper를 통한 확인
            val currentUser = authWrapper.getCurrentUser()
            if (currentUser != null) {
                Log.w("AuthRepositoryImpl", "AuthWrapper still shows user: ${currentUser.uid}")
                return false
            }
            
            // 2. 현재 세션 정보 확인
            when (val sessionResult = getCurrentUserSession()) {
                is CustomResult.Success -> {
                    Log.w("AuthRepositoryImpl", "Session still available after logout: ${sessionResult.data}")
                    return false
                }
                is CustomResult.Failure -> {
                    Log.d("AuthRepositoryImpl", "Session properly cleared: ${sessionResult.error.message}")
                }
                else -> {
                    Log.w("AuthRepositoryImpl", "Unexpected session result after logout: $sessionResult")
                }
            }
            
            // 3. 로그인 상태 확인
            val isLoggedIn = isLoggedIn()
            if (isLoggedIn) {
                Log.w("AuthRepositoryImpl", "isLoggedIn() still returns true after logout")
                return false
            }
            
            Log.d("AuthRepositoryImpl", "Logout verification completed successfully")
            true
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Error during logout verification", e)
            false
        }
    }

    /**
     *
     * @return 성공시 Result.Success(String : uid)
     */
    override suspend fun signup(
        email: String,
        password: String,
    ): CustomResult<String, Exception> {
        return authRemoteDataSource.signUp(email, password)
    }

    /**
     * 비밀번호 재설정 코드를 요청합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * @param email 사용자 이메일
     * @return 성공 시 Result.success(Unit), 실패 시 Result.failure
     */
    override suspend fun requestPasswordResetCode(email: String): CustomResult<Unit, Exception> {
        return authRemoteDataSource.requestPasswordResetCode(email)
    }


    /**
     * 이메일 인증 메일을 전송합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * @return 성공 시 Result.success(Unit), 실패 시 Result.failure
     */
    override suspend fun sendEmailVerification(): CustomResult<Unit, Exception> {
        return authRemoteDataSource.sendEmailVerification()
    }

    /**
     * 이메일 인증 여부를 확인합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * @return 인증 여부 (true: 인증됨, false: 인증되지 않음)
     */
    override suspend fun checkEmailVerification(): CustomResult<Boolean, Exception> {
        return authRemoteDataSource.checkEmailVerification()
    }

    /**
     * 이메일 인증 후 새 비밀번호를 설정합니다.
     * 단순 위임 메소드.
     */
    override suspend fun updatePassword(newPassword: String): CustomResult<Unit, Exception> {
        return authRemoteDataSource.updatePassword(newPassword)
    }

    /**
     * 로그인 오류에 대한 사용자 친화적인 오류 메시지를 반환합니다.
     * @param exception 발생한 예외
     * @return 사용자에게 표시할 오류 메시지
     */
    override suspend fun getLoginErrorMessage(exception: Throwable): String {
        // 일반적인 로그인 오류 메시지 처리
        return when (exception.message) {
            "ERROR_INVALID_EMAIL" -> "유효하지 않은 이메일 형식입니다."
            "ERROR_WRONG_PASSWORD" -> "비밀번호가 일치하지 않습니다."
            "ERROR_USER_NOT_FOUND" -> "등록되지 않은 사용자입니다."
            "ERROR_USER_DISABLED" -> "계정이 비활성화되었습니다."
            "ERROR_TOO_MANY_REQUESTS" -> "너무 많은 로그인 시도가 있었습니다. 잠시 후 다시 시도해주세요."
            "ERROR_OPERATION_NOT_ALLOWED" -> "이메일/비밀번호 로그인이 비활성화되었습니다."
            "ERROR_NETWORK" -> "네트워크 연결을 확인해주세요."
            else -> "로그인 중 오류가 발생했습니다. 다시 시도해주세요."
        }
    }

    /**
     * 회원가입 오류에 대한 사용자 친화적인 오류 메시지를 반환합니다.
     * @param exception 발생한 예외
     * @return 사용자에게 표시할 오류 메시지
     */
    override suspend fun getSignUpErrorMessage(exception: Throwable): String {
        // 회원가입 오류 메시지 처리
        return when (exception.message) {
            "ERROR_INVALID_EMAIL" -> "유효하지 않은 이메일 형식입니다."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "이미 사용 중인 이메일입니다."
            "ERROR_WEAK_PASSWORD" -> "비밀번호가 너무 약합니다. 6자 이상의 강한 비밀번호를 사용해주세요."
            else -> "회원가입 중 오류가 발생했습니다. 다시 시도해주세요."
        }
    }

    /**
     * 비밀번호 재설정 오류에 대한 사용자 친화적인 오류 메시지를 반환합니다.
     * @param exception 발생한 예외
     * @return 사용자에게 표시할 오류 메시지
     */
    override suspend fun getPasswordResetErrorMessage(exception: Throwable): String {
        // 비밀번호 재설정 오류 메시지 처리
        return when {
            exception is com.google.firebase.FirebaseNetworkException -> "네트워크 연결을 확인해주세요."
            exception.message == "ERROR_INVALID_EMAIL" -> "유효하지 않은 이메일 형식입니다."
            exception.message == "ERROR_USER_NOT_FOUND" -> "등록되지 않은 이메일입니다."
            exception.message == "ERROR_INVALID_ACTION_CODE" -> "유효하지 않은 코드입니다. 코드가 만료되었거나 이미 사용되었을 수 있습니다."
            else -> "비밀번호 재설정 중 오류가 발생했습니다. 다시 시도해주세요."
        }
    }


    /**
     * 현재 로그인된 사용자 계정을 삭제합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @return 성공 시 Result.success(Unit), 실패 시 Result.failure
     */
    override suspend fun withdrawCurrentUser(): CustomResult<Unit, Exception> {
        return when(val firebaseUser = authRemoteDataSource.getCurrentUser()) {
            is CustomResult.Success -> {
                firebaseUser.data.delete().await()
                CustomResult.Success(Unit)
            }
            is CustomResult.Failure -> {
                CustomResult.Failure(firebaseUser.error)
            }
            else -> {
                CustomResult.Failure(Exception("Unknown error occurred during login"))
            }
        }
    }

    /**
     * 현재 사용자의 세션 정보를 가져옵니다.
     * Firebase의 현재 사용자 정보를 기반으로 UserSession 객체를 생성합니다.
     * 
     * @return 현재 사용자의 세션 정보가 포함된 CustomResult 또는 null
     */
    override fun getCurrentUserSession(): CustomResult<UserSession, Exception> {
        return when (val result = authRemoteDataSource.getCurrentUser()) {
            is CustomResult.Success -> {
                val firebaseUser = result.data
                val userSession = UserSession(
                    userId = UserId(firebaseUser.uid),
                    email = firebaseUser.email?.let { value -> UserEmail(value) },
                    displayName = firebaseUser.displayName?.let { value -> UserName.from(value) },
                )

                CustomResult.Success(userSession)
            }
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            else -> CustomResult.Failure(Exception("Unknown error occurred during login"))
        }
    }


    /**
     * 현재 사용자의 세션 정보를 실시간으로 관찰합니다.
     * Firebase의 인증 상태 변경을 감지하여 세션 정보를 업데이트합니다.
     *
     * @return 사용자 세션 정보의 Flow
     */
    override fun getUserSessionStream(): Flow<CustomResult<UserSession, Exception>> {
        return authRemoteDataSource.observeAuthState().map { firebaseUser ->
            when (firebaseUser) {
                is CustomResult.Success -> {
                    val userSession = UserSession(
                        userId = UserId(firebaseUser.data.uid),
                        email = firebaseUser.data.email?.let { value -> UserEmail(value) },
                        displayName = firebaseUser.data.displayName?.let { value -> UserName.from(value) },
                    )

                    return@map CustomResult.Success(userSession)
                }

                else -> {
                    // 로그아웃 상태
                    return@map CustomResult.Failure(Exception("No user is currently signed in"))
                }
            }
        }
    }

    /**
     * 신규 구현: ID Token 획득 전용 메서드.
     */
    override suspend fun fetchIdToken(forceRefresh: Boolean): CustomResult<Token, Exception> {
        return when (val firebaseUserResult = authRemoteDataSource.getCurrentUser()) {
            is CustomResult.Success -> {
                try {
                    val tokenResult = firebaseUserResult.data.getIdToken(forceRefresh).await()
                    if ( tokenResult.token.isNullOrEmpty()) {
                        CustomResult.Failure(Exception("No token found"))
                    } else {
                        CustomResult.Success(Token(tokenResult.token!!))
                    }
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }
            is CustomResult.Failure -> CustomResult.Failure(firebaseUserResult.error)
            else -> CustomResult.Failure(Exception("No authenticated user"))
        }
    }
}