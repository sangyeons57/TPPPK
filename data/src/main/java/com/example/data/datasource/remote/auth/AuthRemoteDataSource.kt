package com.example.data.datasource.remote.auth

import com.example.data.model.remote.user.UserDto
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlin.Result
import java.time.Instant

/**
 * Firebase Authentication과의 상호작용을 추상화하는 데이터 소스 인터페이스
 */
interface AuthRemoteDataSource {

    /**
     * 현재 인증된 사용자가 있는지 확인합니다.
     *
     * @return 로그인 상태 여부
     */
    suspend fun isLoggedIn(): Boolean
    
    /**
     * 현재 인증된 사용자 정보를 Flow로 제공합니다.
     *
     * @return 사용자 정보 Flow (Firebase 인증 상태 변경을 실시간으로 반영)
     */
    fun getCurrentUser(): Flow<FirebaseUser?>
    
    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     *
     * @return 사용자 ID 또는 인증된 사용자가 없으면 null
     */
    suspend fun getCurrentUserId(): String?
    
    /**
     * 세션 유효성을 확인하고 사용자 정보를 반환합니다.
     *
     * @return 성공 시 UserDto가 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun checkSession(): Result<UserDto?>

    /**
     * 이메일과 비밀번호로 로그인을 시도합니다.
     *
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 성공 시 UserDto가 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun login(email: String, password: String): Result<UserDto?>
    
    /**
     * 현재 사용자를 로그아웃합니다.
     *
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun logout(): Result<Unit>

    /**
     * 비밀번호 재설정 이메일을 발송합니다.
     *
     * @param email 비밀번호를 재설정할 이메일 주소
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun requestPasswordResetCode(email: String): Result<Unit>
    
    /**
     * 비밀번호 재설정 코드의 유효성을 확인합니다.
     *
     * @param email 사용자 이메일
     * @param code 확인할 코드
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun verifyPasswordResetCode(email: String, code: String): Result<Unit>
    
    /**
     * 새 비밀번호로 재설정합니다.
     *
     * @param email 사용자 이메일
     * @param code 인증된 코드
     * @param newPassword 새 비밀번호
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit>

    /**
     * 이메일, 비밀번호, 닉네임으로 회원가입을 시도합니다.
     *
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @param nickname 사용자 닉네임
     * @param consentTimeStamp 서비스 정책및 개인정보처리방침 동의 시간
     * @return 성공 시 UserDto가 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun signUp(email: String, password: String, nickname: String, consentTimeStamp: Instant): Result<UserDto?>
    
    /**
     * 현재 사용자에게 이메일 인증 메일을 발송합니다.
     *
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun sendEmailVerification(): Result<Unit>
    
    /**
     * 현재 사용자의 이메일 인증 상태를 확인합니다.
     *
     * @return 성공 시 인증 상태(Boolean)가 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun checkEmailVerification(): Result<Boolean>
} 