// 파일 위치: domain/repository/AuthRepository.kt
package com.example.domain.repository

import com.example.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlin.Result
import java.time.Instant

/**
 * 인증 관련 데이터 처리를 위한 인터페이스 (Contract)
 * Firebase Authentication 서비스와의 상호작용을 정의합니다.
 */
interface AuthRepository {

    // --- 로그인/로그아웃/상태 관련 ---
    suspend fun isLoggedIn(): Boolean // 로그인 상태 확인 (Splash)
    
    fun getCurrentUser(): Flow<User?> // 현재 인증된 사용자 정보를 Flow로 가져옴
    
    suspend fun getCurrentUserId(): String? // 현재 인증된 사용자의 ID 반환
    
    suspend fun checkSession(): Result<User?> // 세션 유효성 확인 및 사용자 정보 반환

    suspend fun login(email: String, pass: String): Result<User?> // 로그인 시도 (Login - User 모델 반환)
    
    suspend fun logout(): Result<Unit> // 로그아웃

    // --- 비밀번호 재설정 관련 (FindPassword) ---
    suspend fun requestPasswordResetCode(email: String): Result<Unit>
    suspend fun verifyPasswordResetCode(email: String, code: String): Result<Unit>
    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit>

    // --- 회원가입 관련 (SignUp) ---
    suspend fun signUp(email: String, pass: String, nickname: String, consentTimeStamp: Instant = Instant.now()): Result<User?> // 회원가입
    
    suspend fun sendEmailVerification(): Result<Unit> // 이메일 인증 전송
    
    suspend fun checkEmailVerification(): Result<Boolean> // 이메일 인증 확인

    // --- 비밀번호 변경 관련 ---
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> // 현재 로그인된 사용자의 비밀번호 변경

    // --- 회원 탈퇴 ---
    suspend fun deleteCurrentUser(): Result<Unit> // 현재 Firebase Auth 사용자 삭제

    // --- 에러 처리 ---
    suspend fun getLoginErrorMessage(exception: Throwable): String
    suspend fun getSignUpErrorMessage(exception: Throwable): String
    suspend fun getPasswordResetErrorMessage(exception: Throwable): String
}