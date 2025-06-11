// 파일 위치: domain/repository/AuthRepository.kt
package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.model.base.User
import com.google.api.CustomHttpPatternOrBuilder
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 인증 관련 데이터 처리를 위한 인터페이스 (Contract)
 * Firebase Authentication 서비스와의 상호작용을 정의합니다.
 */
interface AuthRepository {

    // --- 로그인/로그아웃/상태 관련 ---
    suspend fun login(email: String, password: String): CustomResult<UserSession,Exception>
    suspend fun isLoggedIn(): Boolean // 로그인 상태 확인 (Splash)
    
    suspend fun getCurrentUserId(): CustomResult<String, Exception> // 현재 인증된 사용자의 ID 반환
    
    suspend fun logout(): CustomResult<Unit, Exception> // 로그아웃

    suspend fun signup(email: String, password: String): CustomResult<String, Exception> //회원가입

    // --- 비밀번호 재설정 관련 (FindPassword) ---
    suspend fun requestPasswordResetCode(email: String): CustomResult<Unit,  Exception>

    // --- 회원가입 관련 (SignUp) ---
    
    suspend fun sendEmailVerification(): CustomResult<Unit,  Exception> // 이메일 인증 전송
    
    suspend fun checkEmailVerification(): CustomResult<Boolean,  Exception> // 이메일 인증 확인

    /**
     * 이메일 인증 완료 후 새 비밀번호를 설정합니다.
     * FirebaseAuth.currentUser!!.updatePassword(newPassword) 에 대한 래퍼입니다.
     *
     * @param newPassword 새 비밀번호
     * @return 성공 시 [CustomResult.Success], 실패 시 [CustomResult.Failure]
     */
    suspend fun updatePassword(newPassword: String): CustomResult<Unit, Exception>

    suspend fun updateUserName(newDisplayName: String): CustomResult<User, Exception> // 사용자 이름 업데이트

    // --- 회원 탈퇴 ---
    suspend fun withdrawCurrentUser(): CustomResult<Unit,  Exception> // 현재 Firebase Auth 사용자 삭제

    // --- 에러 처리 ---
    suspend fun getLoginErrorMessage(exception: Throwable): String
    suspend fun getSignUpErrorMessage(exception: Throwable): String
    suspend fun getPasswordResetErrorMessage(exception: Throwable): String
    
    // --- 세션 관리 ---
    /**
     * 현재 사용자의 세션 정보를 가져옵니다.
     * 로그인되어 있지 않은 경우 null을 반환합니다.
     * 
     * @return 현재 사용자의 세션 정보가 포함된 CustomResult 또는 null
     */
    suspend fun getCurrentUserSession(): CustomResult<UserSession, Exception>
    
    /**
     * 현재 사용자의 세션 정보를 실시간으로 관찰합니다.
     * 로그인/로그아웃 상태 변화에 따라 값이 업데이트됩니다.
     * 
     * @return 사용자 세션 정보의 Flow
     */
    suspend fun getUserSessionStream(): Flow<CustomResult<UserSession, Exception>>
}