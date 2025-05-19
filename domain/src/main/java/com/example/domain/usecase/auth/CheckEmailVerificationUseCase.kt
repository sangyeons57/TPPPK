package com.example.domain.usecase.auth

import com.example.domain.repository.AuthRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 현재 인증된 사용자의 이메일 인증 상태를 확인하는 UseCase.
 * Firebase Auth에서 사용자의 최신 이메일 인증 상태를 가져옵니다.
 *
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class CheckEmailVerificationUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 현재 사용자의 이메일 인증 상태를 확인합니다.
     *
     * @return 성공 시 이메일 인증 여부([Boolean])를 담은 [Result.success],
     *         실패 시 [Result.failure] Throwable을 반환합니다.
     *         네트워크 오류, 사용자 세션 만료 등의 이유로 실패할 수 있습니다.
     */
    suspend operator fun invoke(): Result<Boolean> {
        return authRepository.checkEmailVerification()
    }
} 