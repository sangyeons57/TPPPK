package com.example.domain.usecase.auth

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case for processing user membership withdrawal.
 * This involves anonymizing user data and signing the user out,
 * but does not delete the user's auth record.
 */
interface WithdrawMembershipUseCase {
    /**
     * Executes the withdrawal process.
     * @return A [CustomResult] indicating success or failure.
     */
    suspend operator fun invoke(): CustomResult<Unit, Exception>
}

/**
 * Implementation for [WithdrawMembershipUseCase].
 * This use case processes user membership withdrawal by anonymizing user data in Firestore,
 * marking the account as withdrawn, and then signing the user out.
 */
class WithdrawMembershipUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : WithdrawMembershipUseCase {

    override suspend operator fun invoke(): CustomResult<Unit, Exception> {
        Log.d("WithdrawMembershipUseCaseImpl", "Attempting user withdrawal process.")
        // 1. Get current user's UID
        val currentUserResult = authRepository.getCurrentUserSession()
        val session = when (currentUserResult) {
            is CustomResult.Success -> currentUserResult.data
            is CustomResult.Failure -> {
                Log.e("WithdrawMembershipUseCaseImpl", "Failed to get current user", currentUserResult.error)
                return CustomResult.Failure(currentUserResult.error ?: Exception("Failed to get current user information."))
            }
            else -> {
                Log.e("WithdrawMembershipUseCaseImpl", "Unknown error while fetching current user")
                return CustomResult.Failure(Exception("Unknown error fetching user information."))
            }
        }

        val uid = session.userId
        Log.d("WithdrawMembershipUseCaseImpl", "Current user UID: $uid. Proceeding with data anonymization.")

        // 2. Process user data withdrawal (anonymize in Firestore)
        when (val processResult = userRepository.processUserWithdrawal(uid)) {
            is CustomResult.Success -> {
                Log.d("WithdrawMembershipUseCaseImpl", "User data anonymized successfully for UID: $uid. Proceeding to sign out.")
                // 3. Sign out the user
                return when (val signOutResult = authRepository.logout()) {
                    is CustomResult.Success -> {
                        Log.d("WithdrawMembershipUseCaseImpl", "User signed out successfully. Withdrawal complete.")
                        CustomResult.Success(Unit)
                    }
                    is CustomResult.Failure -> {
                        Log.e("WithdrawMembershipUseCaseImpl", "Failed to sign out user after data anonymization.", signOutResult.error)
                        // Even if sign-out fails, the data processing part was successful.
                        // Depending on desired behavior, this could still be a partial success or a failure.
                        // For now, returning the sign-out failure.
                        CustomResult.Failure(signOutResult.error ?: Exception("Sign out failed after data processing."))
                    }
                    else -> {
                        Log.e("WithdrawMembershipUseCaseImpl", "Unknown error during sign out.")
                        CustomResult.Failure(Exception("Unknown error during sign out."))
                    }
                }
            }
            is CustomResult.Failure -> {
                Log.e("WithdrawMembershipUseCaseImpl", "Failed to process user data withdrawal for UID: $uid.", processResult.error)
                return CustomResult.Failure(processResult.error ?: Exception("Failed to process user data withdrawal."))
            }
            else -> {
                 Log.e("WithdrawMembershipUseCaseImpl", "Unknown error during data processing for UID: $uid.")
                return CustomResult.Failure(Exception("Unknown error during data processing."))
            }
        }
    }
}
