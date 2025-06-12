package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case for updating the current user's memo (status message).
 */
interface UpdateUserMemoUseCase {
    /**
     * Invokes the use case to update the user's memo.
     *
     * @param newMemo The new memo string to set.
     * @return A [CustomResult] indicating success (Unit) or failure (Exception).
     */
    suspend operator fun invoke(newMemo: String): CustomResult<Unit, Exception>
}

/**
 * Implementation of [UpdateUserMemoUseCase] for updating the user's memo.
 */
class UpdateUserMemoUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : UpdateUserMemoUseCase {

    /**
     * Invokes the use case to update the user's memo.
     *
     * @param newMemo The new memo string to set.
     * @return A [CustomResult] indicating success (Unit) or failure (Exception).
     */
    override suspend operator fun invoke(newMemo: String): CustomResult<Unit, Exception> {
        return userRepository.updateCurrentUserMemo(newMemo)
    }
}
