package com.example.domain.usecase.auth

import com.example.domain.repository.AuthRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 비밀번호 재설정 코드를 요청하는 UseCase
 * 
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class RequestPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 지정된 이메일 주소로 비밀번호 재설정 코드를 요청합니다.
     *
     * @param email 비밀번호 재설정을 요청할 이메일 주소
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(email: String): Result<Unit> {
        return authRepository.requestPasswordResetCode(email)
    }
} 