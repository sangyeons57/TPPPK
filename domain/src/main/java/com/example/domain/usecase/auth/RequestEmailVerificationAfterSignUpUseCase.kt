package com.example.domain.usecase.auth

import com.example.core_common.result.CustomResult
import com.example.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case to request sending an email verification to the currently signed-in user,
 * typically after a successful sign-up.
 *
 * @property authRepository Repository for authentication-related data operations.
 */
class RequestEmailVerificationAfterSignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    /**
     * Invokes the use case to request sending an email verification.
     *
     * @return A [CustomResult] indicating success or failure of the request.
     */
    suspend operator fun invoke(): CustomResult<Unit, Exception> {
        return authRepository.sendEmailVerification()
    }
}
