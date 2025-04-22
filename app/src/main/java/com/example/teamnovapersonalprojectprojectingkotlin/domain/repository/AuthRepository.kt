// 파일 위치: domain/repository/AuthRepository.kt
package com.example.teamnovapersonalprojectprojectingkotlin.domain.repository

import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.User
import com.google.firebase.auth.FirebaseUser
import kotlin.Result // Kotlin Result 클래스 import

/**
 * 인증 관련 데이터 처리를 위한 인터페이스 (Contract)
 */
interface AuthRepository {

    // --- 로그인/로그아웃/상태 관련 ---
    suspend fun isLoggedIn(): Boolean // 로그인 상태 확인 (Splash)

    suspend fun login(email: String, pass: String): Result<User?> // 로그인 시도 (Login - User 모델 반환 예시)
    suspend fun logout(): Result<Unit>


    // --- 비밀번호 재설정 관련 (FindPassword) ---
    suspend fun requestPasswordResetCode(email: String): Result<Unit>
    suspend fun verifyPasswordResetCode(email: String, code: String): Result<Unit>
    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit>

    // --- 회원가입 관련 (SignUp - 필요 함수 예시) ---
    //suspend fun sendAuthCode(email: String): Result<Unit> // 이메일 인증번호 전송
    //suspend fun verifyAuthCode(email: String, code: String): Result<Unit> // 인증번호 확인
    suspend fun signUp(email: String, pass: String, name: String): Result<User?> // 회원가입



}