package com.example.domain.usecase.auth

import com.example.domain.model.User
import com.example.domain.repository.AuthRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자 인증 세션 유효성을 확인하는 UseCase
 * 
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class CheckSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 현재 사용자의 인증 세션이 유효한지 확인하고 유효하면 사용자 정보를 반환합니다.
     * 앱 시작 시 스플래시 화면 등에서 사용됩니다.
     *
     * @return 성공 시 사용자 정보가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(): Result<User?> {
        return authRepository.checkSession()
    }
} 