package com.example.domain.usecase.auth

import com.example.domain.repository.AuthRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 비밀번호 재설정을 수행하는 UseCase
 * 
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 지정된 이메일 주소, 인증 코드, 새 비밀번호를 이용하여 비밀번호를 재설정합니다.
     *
     * @param email 사용자 이메일 주소
     * @param code 인증된 비밀번호 재설정 코드
     * @param newPassword 새로 설정할 비밀번호
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(email: String, code: String, newPassword: String): Result<Unit> {
        return authRepository.resetPassword(email, code, newPassword)
    }
}

/**
 * 로그아웃 처리를 위한 유스케이스 인터페이스
 */
interface LogoutUseCase {
    suspend operator fun invoke(): Result<Unit>
}

/**
 * LogoutUseCase의 구현체
 * @param authRepository 인증 관련 기능을 제공하는 Repository
 */
class LogoutUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository
) : LogoutUseCase {

    /**
     * 유스케이스를 실행하여 로그아웃을 처리합니다.
     * AuthRepository의 logout 함수를 호출합니다.
     * @return Result<Unit> 로그아웃 처리 결과
     */
    override suspend fun invoke(): Result<Unit> {
        return authRepository.logout()
    }
}