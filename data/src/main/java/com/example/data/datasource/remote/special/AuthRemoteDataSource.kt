package com.example.data.datasource.remote.special

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.Email
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRemoteDataSource {

    /**
     * 현재 로그인 상태(사용자)의 변경을 실시간으로 관찰합니다.
     * @return 로그인 시 FirebaseUser, 로그아웃 시 null을 방출하는 Flow
     */
    fun observeAuthState(): Flow<CustomResult<FirebaseUser, Exception>>

    /**
     * 현재 로그인된 FirebaseUser 객체를 즉시 반환합니다. 동기적인 세션 확인에 사용됩니다.
     * @return 로그인 상태이면 FirebaseUser, 아니면 null
     */
    fun getCurrentUser(): CustomResult<FirebaseUser, Exception>
    
    /**
     * 현재 로그인된 사용자의 ID를 가져옵니다.
     * @return 로그인 상태이면 사용자 ID, 아니면 null
     */
    suspend fun getCurrentUserId(): String?

    /**
     * 이메일과 비밀번호로 회원가입을 시도합니다.
     * @param email 가입할 이메일
     * @param password 사용할 비밀번호
     * @return 성공 시 생성된 사용자의 UID를 포함한 CustomResult 객체
     */
    suspend fun signUp(email: String, password: String): CustomResult<String, Exception>

    /**
     * 이메일과 비밀번호로 로그인을 시도합니다.
     * @param email 로그인할 이메일
     * @param password 비밀번호
     * @return 성공 시 로그인된 사용자의 UID를 포함한 CustomResult 객체
     */
    suspend fun signIn(email: Email, password: String): CustomResult<String, Exception>

    /**
     * 현재 로그인된 사용자를 로그아웃합니다.
     */
    suspend fun signOut(): CustomResult<Unit, Exception>
    
    /**
     * 비밀번호 재설정 이메일을 전송합니다.
     * @param email 비밀번호를 재설정할 이메일 주소
     * @return 성공 시 CustomResult.Success(Unit), 실패 시 CustomResult.Failure 반환
     */
    suspend fun requestPasswordResetCode(email: String): CustomResult<Unit, Exception>
    
    /**
     * 현재 로그인된 사용자에게 이메일 인증 메일을 전송합니다.
     * @return 성공 시 CustomResult.Success(Unit), 실패 시 CustomResult.Failure 반환
     */
    suspend fun sendEmailVerification(): CustomResult<Unit, Exception>
    
    /**
     * 현재 로그인된 사용자의 이메일 인증 상태를 확인합니다.
     * @return 이메일이 인증된 경우 CustomResult.Success(true),
     *         인증되지 않은 경우 CustomResult.Success(false),
     *         오류 발생 시 CustomResult.Failure 반환
     */
    suspend fun checkEmailVerification(): CustomResult<Boolean, Exception>

    /**
     * 현재 로그인한 사용자의 비밀번호를 업데이트합니다.
     * FirebaseAuth.currentUser!!.updatePassword(newPassword)의 래퍼입니다.
     *
     * @param newPassword 새 비밀번호
     * @return 성공 시 [CustomResult.Success], 실패 시 [CustomResult.Failure]
     */
    suspend fun updatePassword(newPassword: String): CustomResult<Unit, Exception>
}
@Singleton
class AuthRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRemoteDataSource {

    override fun observeAuthState(): Flow<CustomResult<FirebaseUser, Exception>> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                trySend(CustomResult.Failure(Exception("No user is currently signed in")))
            } else {
                trySend(CustomResult.Success(firebaseAuth.currentUser!!))
            }
        }
        auth.addAuthStateListener(authStateListener)

        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    override fun getCurrentUser(): CustomResult<FirebaseUser, Exception> {
        return auth.currentUser?.let { CustomResult.Success(it) } ?: CustomResult.Failure(Exception("No user is currently signed in"))
    }

    override suspend fun getCurrentUserId(): String? = withContext(Dispatchers.IO) {
        auth.currentUser?.uid
    }

    override suspend fun signUp(
        email: String,
        password: String
    ): CustomResult<String, Exception> =
        withContext(Dispatchers.IO) {
            resultTry {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                authResult.user?.uid ?: throw Exception("Failed to create user: UID is null.")
            }
        }

    override suspend fun signIn(email: Email, password: String): CustomResult<String, Exception> =
        withContext(Dispatchers.IO) {
            resultTry {
                val authResult = auth.signInWithEmailAndPassword(email.value, password).await()
                authResult.user?.uid ?: throw Exception("Failed to sign in: UID is null.")
            }
        }

    override suspend fun signOut(): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            auth.signOut()
            Unit
        }
    }

    private inline fun <T> resultTry(block: () -> T): CustomResult<T, Exception> {
        return try {
            CustomResult.Success(block())
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun requestPasswordResetCode(email: String): CustomResult<Unit, Exception> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun sendEmailVerification(): CustomResult<Unit, Exception> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val result = withTimeoutOrNull(10000) {
                    currentUser.sendEmailVerification().await()
                }
                if (result != null) {
                    CustomResult.Success(Unit)
                } else {
                    CustomResult.Failure(Exception("Email verification request timed out"))
                }
            } else {
                CustomResult.Failure(Exception("No user is currently signed in"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun checkEmailVerification(): CustomResult<Boolean, Exception> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Reload the user to get the latest email verification status (10초 타임아웃)
                val reloadResult = withTimeoutOrNull(10000) {
                    currentUser.reload().await()
                }
                if (reloadResult != null) {
                    CustomResult.Success(currentUser.isEmailVerified)
                } else {
                    CustomResult.Failure(Exception("Email verification check timed out"))
                }
            } else {
                CustomResult.Failure(Exception("No user is currently signed in"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 비밀번호 업데이트 구현
     */
    override suspend fun updatePassword(newPassword: String): CustomResult<Unit, Exception> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val result = withTimeoutOrNull(10000) {
                    currentUser.updatePassword(newPassword).await()
                }
                if (result != null) {
                    CustomResult.Success(Unit)
                } else {
                    CustomResult.Failure(Exception("Password update timed out"))
                }
            } else {
                CustomResult.Failure(Exception("No user is currently signed in"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
