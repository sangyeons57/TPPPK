package com.example.domain.usecase.auth.session

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.AuthRepository
import javax.inject.Inject

/**
 * Use case for logging out the current user.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the logout operation.
     * @return A [CustomResult] indicating success (Unit) or an [Exception] on failure.
     */
    suspend operator fun invoke(): CustomResult<Unit, Exception> {
        return authRepository.logout()
    }
}
