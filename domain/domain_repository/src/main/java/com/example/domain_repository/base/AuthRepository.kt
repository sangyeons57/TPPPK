// 파일 위치: domain/repository/AuthRepository.kt
package com.example.domain_repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.user.UserEmail
import com.example.domain_repository.Repository
import kotlinx.coroutines.flow.Flow

/**
 * 인증 관련 데이터 처리를 위한 인터페이스 (Contract)
 * Firebase Authentication 서비스와의 상호작용을 정의합니다.
 */
interface AuthRepository : Repository {

    // --- 로그인/로그아웃/상태 관련 ---
    suspend fun login(email: UserEmail, password: String): CustomResult<UserSession, Exception>
    suspend fun isLoggedIn(): Boolean // 로그인 상태 확인 (Splash)

    suspend fun logout(): CustomResult<Unit, Exception> // 기본 로그아웃
    suspend fun logoutCompletely(): CustomResult<Unit, Exception> // 완전 로그아웃 (캐시 포함)

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


    // --- 회원 탈퇴 ---
    suspend fun withdrawCurrentUser(): CustomResult<Unit,  Exception> // 현재 Firebase Auth 사용자 삭제

    // --- 에러 처리 ---
    suspend fun getLoginErrorMessage(exception: Throwable): String
    suspend fun getSignUpErrorMessage(exception: Throwable): String
    suspend fun getPasswordResetErrorMessage(exception: Throwable): String
    
    // --- 세션 관리 ---
    /**
     * 현재 사용자의 세션 정보를 가져옵니다.
     * 이 메서드는 ID 토큰을 포함한 세션 정보를 반환합니다.
     * 토큰이 만료되었거나 forceRefresh가 true인 경우, 토큰을 갱신합니다.
     *
     * @param forceRefresh 토큰을 강제로 재발급할지 여부 (기본값: false)
     * @return 토큰이 포함된 사용자 세션 정보
     */
    suspend fun getCurrentUserSession(forceRefresh: Boolean = false): CustomResult<UserSession, Exception>

    /**
     * 현재 사용자의 세션 정보를 실시간으로 관찰합니다.
     * 로그인/로그아웃 상태 변화에 따라 값이 업데이트됩니다.
     *
     * @return 사용자 세션 정보의 Flow
     */
    fun getCurrentUserSessionStream(): Flow<CustomResult<UserSession, Exception>>

    /**
     * 토큰을 강제로 갱신합니다.
     * 토큰 만료 임박 시나 인증 오류 발생 시 사용됩니다.
     *
     * @return 갱신된 사용자 세션 정보
     */
    suspend fun refreshToken(): CustomResult<UserSession, Exception>
}