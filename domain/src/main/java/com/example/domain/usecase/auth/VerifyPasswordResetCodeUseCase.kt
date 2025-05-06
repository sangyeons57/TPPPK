package com.example.domain.usecase.auth

import com.example.domain.repository.AuthRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 비밀번호 재설정 코드의 유효성을 확인하는 UseCase
 * 
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class VerifyPasswordResetCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 지정된 이메일 주소와 코드를 이용하여 비밀번호 재설정 코드의 유효성을 검증합니다.
     *
     * @param email 사용자 이메일 주소
     * @param code 검증할 비밀번호 재설정 코드
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(email: String, code: String): Result<Unit> {
        return authRepository.verifyPasswordResetCode(email, code)
    }
} 