package com.example.data.util

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirebaseAuth를 래핑하는 클래스
 * 
 * 이 클래스는 FirebaseAuth 기능을 테스트하기 쉽게 래핑합니다.
 * Mockito로 이 클래스를 테스트할 수 있습니다.
 * 인증 상태 변경을 감지하고 Flow로 제공하는 기능을 포함합니다.
 */
@Singleton
class FirebaseAuthWrapper @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * 이메일/비밀번호로 로그인
     * 
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 성공 시 FirebaseUser, 실패 시 예외
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Result.success(firebaseUser)
            } else {
                Result.failure(Exception("로그인 후 사용자 정보를 가져올 수 없습니다."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 새 계정 생성
     *
     * @param email 사용자 이메일
     * @param password
     * @return 성공 시 FirebaseUser, 실패 시 예외
     */
    suspend fun createUserWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Result.success(firebaseUser)
            } else {
                Result.failure(Exception("회원가입 후 사용자 정보를 가져올 수 없습니다."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 비밀번호 재설정 이메일 전송
     *
     * @param email 사용자 이메일
     * @return 성공 시 Unit, 실패 시 예외
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 현재 로그인된 사용자 반환
     *
     * @return 로그인된 사용자 또는 null
     */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
    
    /**
     * 로그아웃
     */
    fun signOut() {
        firebaseAuth.signOut()
    }
    
    /**
     * Firebase 사용자의 인증 상태 변경을 Flow로 제공합니다.
     * 사용자가 로그인하거나 로그아웃할 때 새로운 FirebaseUser 객체 (또는 null)를 emit 합니다.
     */
    fun authStateChanges(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser).isSuccess // 최신 사용자 상태 전송
        }
        firebaseAuth.addAuthStateListener(authStateListener) // 리스너 등록

        // 초기 상태 전송
        trySend(firebaseAuth.currentUser).isSuccess

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener) // Flow가 close될 때 리스너 제거
        }
    }
}