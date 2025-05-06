package com.example.domain.usecase.auth

import com.example.domain.repository.AuthRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자 비밀번호를 변경하는 UseCase
 * 
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 사용자의 비밀번호를 변경합니다.
     * 새 비밀번호의 유효성을 검사하고 복잡성 요구사항을 확인합니다.
     *
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     * @return 성공 시 성공 결과가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit> {
        // 입력 검증
        if (currentPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("현재 비밀번호를 입력해주세요."))
        }
        
        if (newPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("새 비밀번호를 입력해주세요."))
        }
        
        // 새 비밀번호 길이 검증
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("비밀번호는 최소 6자 이상이어야 합니다."))
        }
        
        // 현재 비밀번호와 새 비밀번호가 같은지 검증
        if (currentPassword == newPassword) {
            return Result.failure(IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다."))
        }
        
        // 비밀번호 복잡성 검증 (예: 숫자, 문자 혼합)
        val hasLetter = newPassword.any { it.isLetter() }
        val hasDigit = newPassword.any { it.isDigit() }
        
        if (!hasLetter || !hasDigit) {
            return Result.failure(IllegalArgumentException("비밀번호는 최소 하나의 문자와 숫자를 포함해야 합니다."))
        }
        
        // 모든 검증 통과 시 Repository 호출
        return authRepository.changePassword(currentPassword, newPassword)
    }
} 