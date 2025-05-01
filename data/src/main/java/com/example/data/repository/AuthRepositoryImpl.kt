package com.example.teamnovapersonalprojectprojectingkotlin.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth // ktx 임포트
import com.google.firebase.ktx.Firebase
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.User
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.AuthRepository
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.UserRepository
import com.example.teamnovapersonalprojectprojectingkotlin.util.SentryUtil
import com.example.teamnovapersonalprojectprojectingkotlin.util.FirestoreConstants as FC
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await // await() 사용 위해 임포트
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth, // FirebaseAuth 주입
    private val firestore: FirebaseFirestore, // Firestore 주입
    private val userRepository: UserRepository, // UserRepository 주입
    // TODO: Firestore 등 다른 서비스 주입 (사용자 정보 저장 시)
) : AuthRepository {

    override suspend fun isLoggedIn(): Boolean {
        Log.d("AuthRepositoryImpl", ""+ auth.currentUser)
        return auth.currentUser != null
    }

    override suspend fun login(email: String, pass: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                // ★ 로그인 성공 후 Firestore 문서 확인 및 생성 ★
                userRepository.ensureUserProfileExists(firebaseUser).fold(
                    onSuccess = { userProfile -> Result.success(userProfile) }, // 성공 시 User 객체 반환
                    onFailure = { exception -> Result.failure(exception) }     // 실패 시 에러 반환
                )
            } else {
                Result.failure(Exception("로그인 후 사용자 정보를 가져올 수 없습니다."))
            }
        } catch (e: Exception) {
            println("Login failed: ${e.message}")
            Result.failure(e) // Firebase 예외 반환
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun requestPasswordResetCode(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            println("Password reset email failed: ${e.message}")
            Result.failure(e)
        }
    }

    // verifyPasswordResetCode는 Firebase SDK에서 직접 제공하지 않음.
    // 비밀번호 재설정은 이메일 링크를 통해 사용자가 직접 하도록 유도하는 것이 일반적.
    // 만약 코드를 사용해야 한다면 Firebase Functions 등 별도 구현 필요.
    // 여기서는 해당 함수를 구현하지 않거나 에러 반환.
    override suspend fun verifyPasswordResetCode(email: String, code: String): Result<Unit> {
        return Result.failure(NotImplementedError("Firebase SDK는 코드 기반 비밀번호 재설정 확인을 직접 지원하지 않습니다."))
    }

    // resetPassword 함수도 위와 같은 이유로 구현 방식 재고 필요.
    // 비밀번호 재설정 링크를 통해 사용자가 직접 변경하도록 유도.
    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> {
        return Result.failure(NotImplementedError("Firebase SDK는 코드 기반 비밀번호 재설정을 직접 지원하지 않습니다."))
    }

    // 회원가입 시 이메일 인증 코드 전송/확인 대신, 가입 후 이메일 확인 메일 발송 방식으로 변경 고려
    /**
    override suspend fun sendAuthCode(email: String): Result<Unit> {
        return Result.failure(NotImplementedError("이메일 확인 메일 발송 방식으로 대체 고려"))
    }

    override suspend fun verifyAuthCode(email: String, code: String): Result<Unit> {
        return Result.failure(NotImplementedError("이메일 확인 메일 발송 방식으로 대체 고려"))
    }
    **/

    override suspend fun signUp(email: String, pass: String, name: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                // (선택) Firebase Auth 프로필에 이름 업데이트
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = name
                }
                try {
                    firebaseUser.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    println("Warning: Failed to update Firebase Auth profile display name: ${e.message}")
                }

                val userDocumentRef = firestore.collection(FC.Collections.USERS).document(firebaseUser.uid) // Auth UID를 문서 ID로 사용

                val newUser = User(
                    userId = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = name,
                    profileImageUrl = null,
                    status = null,
                )

                try {
                    userDocumentRef.set(newUser).await()
                } catch (e: Exception) {
                    println("Warning: Failed to save user data to Firestore: ${e.message}")
                }

                // *** 여기서 이메일 확인 메일 발송 ***
                try {
                    firebaseUser.sendEmailVerification().await()
                    println("Verification email sent to ${firebaseUser.email}")
                } catch (e: Exception) {
                    // 이메일 발송 실패는 회원가입 실패로 처리하지 않을 수도 있음 (로깅 등)
                    println("Failed to send verification email: ${e.message}")
                }


                Result.success(firebaseUser.toDomainUser(name)) // 이름 정보 포함하여 User 모델 매핑
            } else {
                Result.failure(Exception("회원가입 후 사용자 정보를 가져올 수 없습니다."))
            }
        } catch (e: Exception) {
            println("SignUp failed: ${e.message}")
            Result.failure(e)
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
}