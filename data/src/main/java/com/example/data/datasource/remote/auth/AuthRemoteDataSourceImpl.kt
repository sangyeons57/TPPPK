package com.example.data.datasource.remote.auth

import com.example.data.datasource.remote.user.UserRemoteDataSource
import com.example.data.model.remote.user.UserDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.example.core_common.util.DateTimeUtil
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.Result
import java.time.Instant

/**
 * Firebase Authentication을 사용하여 AuthRemoteDataSource를 구현
 */
class AuthRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userRemoteDataSource: UserRemoteDataSource
) : AuthRemoteDataSource {

    // users 컬렉션 참조
    private val usersCollection = firestore.collection("users")

    override suspend fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    override fun getCurrentUser(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        
        auth.addAuthStateListener(authStateListener)
        
        // Initial value
        trySend(auth.currentUser)
        
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override suspend fun checkSession(): Result<UserDto?> = runCatching {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                // Firestore에서 사용자 정보 가져오기
                val documentSnapShot = usersCollection.document(currentUser.uid).get().await()
                if (documentSnapShot.exists()) {
                    documentSnapShot.toObject(UserDto::class.java)
                } else {
                    // 문서가 없으면 기본 UserDto 생성
                    UserDto(
                        id = currentUser.uid,
                        email = currentUser.email ?: "",
                        name = currentUser.displayName ?: "",
                        isEmailVerified = currentUser.isEmailVerified,
                        createdAt = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())
                    )
                }
            } catch (e: Exception) {
                throw e
            }
        } else {
            null
        }
    }

    override suspend fun login(email: String, password: String): Result<UserDto?> = runCatching {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val user = authResult.user
        
        if (user != null) {
            // Firestore에서 사용자 정보 가져오기
            val document = usersCollection.document(user.uid).get().await()
            if (document.exists()) {
                document.toObject(UserDto::class.java)
            } else {
                // 문서가 없으면 기본 UserDto 생성
                UserDto(
                    id = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: "",
                    isEmailVerified = user.isEmailVerified,
                    createdAt = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())
                )
            }
        } else {
            null
        }
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        auth.signOut()
    }

    override suspend fun requestPasswordResetCode(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun verifyPasswordResetCode(email: String, code: String): Result<Unit> = runCatching {
        auth.verifyPasswordResetCode(code).await()
        Unit
    }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String
    ): Result<Unit> = runCatching {
        auth.confirmPasswordReset(code, newPassword).await()
    }

    override suspend fun signUp(
        email: String,
        password: String,
        nickname: String,
        consentTimeStamp: Instant
    ): Result<UserDto?> = runCatching {
        // Firebase Authentication에 사용자 생성
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user
        
        if (user != null) {
            // 닉네임 설정을 위한 프로필 업데이트
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(nickname)
                .build()
            
            user.updateProfile(profileUpdates).await()
            
            // Firestore에 사용자 정보 저장
            val nowTimestamp = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())
            val consentTimestampFirebase = DateTimeUtil.instantToFirebaseTimestamp(consentTimeStamp)
            val userDto = UserDto(
                id = user.uid,
                email = email,
                name = nickname,
                isEmailVerified = user.isEmailVerified,
                createdAt = nowTimestamp,
                consentTimeStamp = consentTimestampFirebase
            )
            
            usersCollection.document(user.uid).set(userDto).await()
            userDto
        } else {
            null
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        val user = auth.currentUser
        if (user != null) {
            user.sendEmailVerification().await()
        } else {
            throw IllegalStateException("No user logged in")
        }
    }

    override suspend fun checkEmailVerification(): Result<Boolean> = runCatching {
        // 사용자 정보 리로드하여 최신 상태 확인
        val user = auth.currentUser
        if (user != null) {
            user.reload().await()
            user.isEmailVerified
        } else {
            throw IllegalStateException("No user logged in")
        }
    }
} 