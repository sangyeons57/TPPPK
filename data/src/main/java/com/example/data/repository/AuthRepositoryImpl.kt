package com.example.data.repository

import android.util.Log
import com.example.core_logging.SentryUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.domain.model.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import com.example.data.util.FirebaseAuthWrapper
import com.example.data.util.FirestoreConstants as FC
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await // await() 사용 위해 임포트
import javax.inject.Inject
import com.example.data.datasource.remote.auth.AuthRemoteDataSource
import com.example.data.model.mapper.UserMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.Result

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth, // FirebaseAuth 주입
    private val authWrapper: FirebaseAuthWrapper, // 추가: FirebaseAuthWrapper 주입
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val userMapper: UserMapper
) : AuthRepository {

    override suspend fun isLoggedIn(): Boolean {
        Log.d("AuthRepositoryImpl", ""+ auth.currentUser)
        return authWrapper.getCurrentUser() != null
    }

    override fun getCurrentUser(): Flow<User?> {
        return authRemoteDataSource.getCurrentUser().map { firebaseUser ->
            if (firebaseUser != null) {
                val userDto = authRemoteDataSource.checkSession().getOrNull()
                userDto?.let { userMapper.mapToDomain(it) }
            } else {
                null
            }
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return authRemoteDataSource.getCurrentUserId()
    }

    override suspend fun checkSession(): Result<User?> {
        return authRemoteDataSource.checkSession().map { userDto ->
            userDto?.let { userMapper.mapToDomain(it) }
        }
    }

    override suspend fun login(email: String, pass: String): Result<User?> {
        return authRemoteDataSource.login(email, pass).map { userDto ->
            userDto?.let { userMapper.mapToDomain(it) }
        }
    }

    override suspend fun logout(): Result<Unit> {
        return authRemoteDataSource.logout()
    }

    override suspend fun requestPasswordResetCode(email: String): Result<Unit> {
        return authRemoteDataSource.requestPasswordResetCode(email)
    }

    override suspend fun verifyPasswordResetCode(email: String, code: String): Result<Unit> {
        return authRemoteDataSource.verifyPasswordResetCode(email, code)
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> {
        return authRemoteDataSource.resetPassword(email, code, newPassword)
    }

    override suspend fun signUp(email: String, pass: String, nickname: String): Result<User?> {
        return authRemoteDataSource.signUp(email, pass, nickname).map { userDto ->
            userDto?.let { userMapper.mapToDomain(it) }
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return authRemoteDataSource.sendEmailVerification()
    }

    override suspend fun checkEmailVerification(): Result<Boolean> {
        return authRemoteDataSource.checkEmailVerification()
    }

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
            else -> "로그인에 실패했습니다: ${exception.message}"
        }
    }

    override suspend fun getSignUpErrorMessage(exception: Throwable): String {
        // 일반적인 회원가입 오류 메시지 처리
        return when (exception.message) {
            "ERROR_INVALID_EMAIL" -> "유효하지 않은 이메일 형식입니다."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "이미 사용 중인 이메일입니다."
            "ERROR_WEAK_PASSWORD" -> "비밀번호가 너무 약합니다. 6자 이상의 비밀번호를 사용해주세요."
            "ERROR_NETWORK" -> "네트워크 연결을 확인해주세요."
            else -> "회원가입에 실패했습니다: ${exception.message}"
        }
    }

    override suspend fun getPasswordResetErrorMessage(exception: Throwable): String {
        return when (exception) {
            is com.google.firebase.FirebaseNetworkException -> "네트워크 연결을 확인해주세요."
            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "존재하지 않는 이메일입니다."
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "올바른 이메일 형식이 아닙니다."
            is com.google.firebase.FirebaseTooManyRequestsException -> "너무 많은 요청이 발생했습니다. 잠시 후 다시 시도해주세요."
            else -> "비밀번호 재설정 요청 중 오류가 발생했습니다: ${exception.message}"
        }
    }

    // FirebaseUser를 Domain User 모델로 변환하는 확장 함수 (AuthRepositoryImpl 내부 또는 별도 파일)
    private fun FirebaseUser.toDomainUser(defaultName: String? = null): User {
        return User(
            userId = this.uid,
            email = this.email ?: "",
            name = this.displayName ?: defaultName ?: "Unknown" // Firebase 프로필 이름 또는 가입 시 이름 사용
        )
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        // 현재 로그인된 사용자 확인
        val user = auth.currentUser ?: return Result.failure(
            IllegalStateException("비밀번호를 변경하려면 로그인해야 합니다.")
        )
        
        return try {
            // 현재 비밀번호 검증 (Firebase는 현재 비밀번호 확인 API가 없으므로, 재인증 필요)
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                user.email ?: "", currentPassword
            )
            
            // 재인증 시도
            user.reauthenticate(credential).await()
            
            // 재인증 성공 시 새 비밀번호로 업데이트
            user.updatePassword(newPassword).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            // 오류 처리 및 사용자 친화적인 메시지 반환
            val errorMessage = when (e.message) {
                "ERROR_WEAK_PASSWORD" -> "비밀번호가 너무 약합니다. 6자 이상의 비밀번호를 사용해주세요."
                "ERROR_REQUIRES_RECENT_LOGIN" -> "민감한 작업을 위해 최근 로그인이 필요합니다. 다시 로그인해주세요."
                "ERROR_WRONG_PASSWORD" -> "현재 비밀번호가 일치하지 않습니다."
                else -> "비밀번호 변경 실패: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        }
    }
}