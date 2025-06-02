package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case for changing the user's memo (status message).
 */
class ChangeMemoUseCase @Inject constructor(
    private val userRepository: UserRepository // Assuming UserRepository has a method to update memo
) {
    suspend operator fun invoke(newMemo: String): CustomResult<Unit, Exception> {
        // Placeholder implementation - actual logic will call userRepository.updateUserMemo(newMemo)
        // For now, let's return a success to allow compilation and UI testing flow.
        // return userRepository.updateUserMemo(newMemo) 
        println("ChangeMemoUseCase invoked with: $newMemo (Placeholder - returning success)")
        return CustomResult.Success(Unit) 
    }
}
