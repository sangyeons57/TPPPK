package com.example.domain.usecase.auth

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.AuthRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 현재 인증된 사용자에게 이메일 인증 메일을 발송하는 UseCase.
 * Firebase Auth를 통해 현재 로그인된 사용자에게 인증 메일을 보냅니다.
 *
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class SendEmailVerificationUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 이메일 인증 메일을 발송합니다.
     *
     * @return 성공 시 [Result.success] Unit, 실패 시 [Result.failure] Throwable을 반환합니다.
     *         네트워크 오류, 사용자 세션 만료 등의 이유로 실패할 수 있습니다.
     */
    suspend operator fun invoke(): CustomResult<Unit, Exception> {
        return authRepository.sendEmailVerification()
    }
} 