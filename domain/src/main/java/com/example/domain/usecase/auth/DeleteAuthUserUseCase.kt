package com.example.domain.usecase.auth

import com.example.core_common.result.CustomResult
import com.example.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for permanently deleting the current Firebase Authentication user.
 * This is a destructive operation.
 */
class DeleteAuthUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the Firebase Auth user deletion.
     */
    suspend operator fun invoke(): CustomResult<Unit, Exception> {
        return authRepository.withdrawCurrentUser()
    }
}
