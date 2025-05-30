package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import com.google.api.CustomHttpPatternOrBuilder
import javax.inject.Inject

interface WithdrawUserUseCase {
    suspend operator fun invoke(): CustomResult<Unit, Exception>
}

class WithdrawUserUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : WithdrawUserUseCase {

    override suspend operator fun invoke(): CustomResult<Unit, Exception> {
        return try {
            // Step 1: Delete the user from Firebase Authentication
            authRepository.deleteCurrentUser().getOrThrow()

            // Step 2: Clear sensitive user data and mark as withdrawn in Firestore/RTDB
            userRepository.clearSensitiveUserDataAndMarkAsWithdrawn().getOrThrow()

            CustomResult.Success(Unit)
        } catch (e: Exception) {
            // TODO: Add more specific error handling if needed
            CustomResult.Failure(e)
        }
    }
}
