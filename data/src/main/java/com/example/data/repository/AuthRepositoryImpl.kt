package com.example.data.repository

import android.util.Log
import com.example.data.datasource.remote.AuthRemoteDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.domain.model.auth.AuthenticationState
import com.example.domain.repository.AuthRepository
import com.example.data.util.FirebaseAuthWrapper
import kotlinx.coroutines.tasks.await // await() 사용 위해 임포트
import javax.inject.Inject
import com.example.data.model.mapper.UserMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.Instant
import com.example.domain.model.base.User
import com.example.core_common.result.CustomResult
import com.example.domain.model.UserSession
import io.sentry.MeasurementUnit
import kotlinx.coroutines.flow.map

/**
 * Firebase Authentication을 사용하여 AuthRepository 인터페이스를 구현한 클래스
 * 인증 관련 작업과 세션 관리를 담당합니다.
 */
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth, // FirebaseAuth 주입
    private val authWrapper: FirebaseAuthWrapper, // 추가: FirebaseAuthWrapper 주입
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val userMapper: UserMapper
) : AuthRepository {


    /**
     * 이메일과 비밀번호로 로그인합니다.
     * 성공 시 UserSession 객체를 반환합니다.
     * 
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 성공 시 UserSession이 포함된 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun login(email: String, password: String): CustomResult<UserSession, Exception> {
        // 로그인 시도
        val loginResult = authRemoteDataSource.signIn(email, password)
        
        return when (loginResult) {
            is CustomResult.Success -> {
                // 로그인 성공 시 세션 정보 가져오기
                val userSessionResult = getCurrentUserSession()
                
                when (userSessionResult) {
                    is CustomResult.Success -> {
                        if (userSessionResult.data != null) {
                            CustomResult.Success(userSessionResult.data!!)
                        } else {
                            // 세션 정보를 가져올 수 없는 경우 (로그인은 성공했지만 세션 정보가 없는 상황)
                            CustomResult.Failure(Exception("Failed to get user session after login"))
                        }
                    }
                    is CustomResult.Failure -> {
                        // 세션 정보를 가져오는 데 실패한 경우
                        CustomResult.Failure(userSessionResult.error)
                    }
                    else -> {
                        CustomResult.Failure(Exception("Unknown error occurred during login"))
                    }
                }
            }
            is CustomResult.Failure -> {
                // 로그인 실패 시 오류 그대로 전달
                CustomResult.Failure(loginResult.error)
            }
            else -> {
                CustomResult.Failure(Exception("Unknown error occurred during login"))
            }
        }
    }
    /**
     * 로그인 상태를 확인합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * @return 로그인 상태 (true: 로그인됨, false: 로그아웃됨)
     */
    override suspend fun isLoggedIn(): Boolean {
        Log.d("AuthRepositoryImpl", ""+ auth.currentUser)
        return authWrapper.getCurrentUser() != null
    }

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * @return 사용자 ID 또는 null(로그아웃 상태일 경우)
     */
    override suspend fun getCurrentUserId(): CustomResult<String, Exception> {
        val user = authRemoteDataSource.getCurrentUser()

        return when (user) {
            is CustomResult.Success -> {
                CustomResult.Success(user.data.uid)
            }
            else -> {
                CustomResult.Failure(Exception("User not logged in"))
            }
        }
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
     *
     * @return 성공시 Result.Success(String : uid)
     */
    override suspend fun signup(
        email: String,
        password: String,
    ): CustomResult<String, Exception> {
        return authRemoteDataSource.signUp(email, password);
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
     * FirebaseUser를 Domain User 모델로 변환하는 확장 함수
     * 
     * @param defaultName 표시이름이 없을 경우 사용할 기본 이름
     * @return 변환된 User 도메인 모델
     */
    private fun FirebaseUser.toDomainUser(defaultName: String? = null): User {
        return User(
            uid = this.uid,
            email = this.email ?: "",
            name = this.displayName ?: defaultName ?: "Anonymous",
            profileImageUrl = this.photoUrl?.toString()
        )
    }

    /**
     * 현재 로그인된 사용자 계정을 삭제합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     * 
     * @return 성공 시 Result.success(Unit), 실패 시 Result.failure
     */
    override suspend fun deleteCurrentUser(): CustomResult<Unit, Exception> {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            return CustomResult.Failure(Exception("User not logged in"))
        }
        return try {
            firebaseUser.delete().await()
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            // Log the exception for debugging purposes
            Log.e("AuthRepositoryImpl", "Error deleting user: ${e.message}", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateUserName(newDisplayName: String): CustomResult<User, Exception> {
        // 1. 현재 Firebase 사용자 가져오기
        val user = FirebaseAuth.getInstance().currentUser

        // 사용자가 로그인되어 있는지 확인
        if (user != null) {
            // 2. UserProfileChangeRequest 객체 생성
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newDisplayName)
                // .setPhotoUri(newPhotoUri) // 프로필 사진도 변경하려면 여기에 추가
                .build()

            // 3. updateProfile() 호출하여 프로필 업데이트 요청
            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    // 4. Task 결과 처리
                    if (task.isSuccessful) {
                        Log.d(TAG, "User display name updated successfully.")
                        Log.d(TAG, "New display name: ${FirebaseAuth.getInstance().currentUser?.displayName}")
                        // 성공 시 UI 업데이트 또는 사용자에게 알림 등의 추가 작업 수행
                        // 예: viewModel.updateUserNameInUi(newDisplayName)
                        return CustomResult.Success(FirebaseAuth.getInstance().currentUser?.toDomainUser())
                    } else {
                        Log.e(TAG, "Failed to update user display name.", task.exception)
                        // 실패 시 사용자에게 오류 메시지 표시 등의 작업 수행
                        return CustomResult.Failure(task.exception)
                    }
                }
        } else {
            return CustomResult.Failure(Exception("User not logged in"))
        }
    }
    
    /**
     * 현재 사용자의 세션 정보를 가져옵니다.
     * Firebase의 현재 사용자 정보를 기반으로 UserSession 객체를 생성합니다.
     * 
     * @return 현재 사용자의 세션 정보가 포함된 CustomResult 또는 null
     */
    override suspend fun getCurrentUserSession(): CustomResult<UserSession, Exception> {
        return auth.currentUser.let {
            when (it) {
                is FirebaseUser -> {
                    // 로그인된 상태 - 토큰 가져오기
                    val tokenResult = try {
                        it.getIdToken(false).await()
                    } catch (e: Exception) {
                        // 토큰 가져오기 실패 시 빈 토큰으로 처리
                        Log.w("AuthRepositoryImpl", "Failed to get token: ${e.message}")
                        null
                    }

                    val userSession = UserSession(
                        userId = it.uid,
                        token = tokenResult?.token ?: "",
                        email = it.email,
                        displayName = it.displayName,
                        photoUrl = it.photoUrl?.toString()
                    )

                    CustomResult.Success(userSession)
                }

                else -> {
                    CustomResult.Failure(Exception("No user is currently signed in"))
                }
            }
        }
    }


    /**
     * 현재 사용자의 세션 정보를 실시간으로 관찰합니다.
     * Firebase의 인증 상태 변경을 감지하여 세션 정보를 업데이트합니다.
     *
     * @return 사용자 세션 정보의 Flow
     */
    override suspend fun getUserSessionStream(): Flow<CustomResult<UserSession, Exception>> {
        TODO("Not yet implemented")
        return authRemoteDataSource.observeAuthState().map { firebaseUser ->
            when (firebaseUser) {
                is CustomResult.Success -> {
                    // 로그인 상태 - 토큰 가져오기
                    val tokenResult = try {
                        firebaseUser.data.getIdToken(false).await()
                    } catch (e: Exception) {
                        // 토큰 가져오기 실패 시 빈 토큰으로 처리
                        Log.w(
                            "AuthRepositoryImpl",
                            "Failed to get token in stream: ${e.message}"
                        )
                        null
                    }

                    val userSession = UserSession(
                        userId = firebaseUser.data.uid,
                        token = tokenResult?.token ?: "",
                        email = firebaseUser.data.email,
                        displayName = firebaseUser.data.displayName,
                        photoUrl = firebaseUser.data.photoUrl?.toString()
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
}