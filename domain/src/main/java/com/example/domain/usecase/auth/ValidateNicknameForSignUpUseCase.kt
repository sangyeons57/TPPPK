package com.example.domain.usecase.auth

import com.example.core_common.result.CustomResult
import com.example.domain.model.ui.auth.UserNameResult
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.first
import java.util.NoSuchElementException
import javax.inject.Inject

/**
 * Use case to validate a nickname for the sign-up process.
 * It checks for emptiness, length, allowed characters, and availability.
 *
 * @property userRepository Repository for user-related data operations.
 */
class ValidateNicknameForSignUpUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    companion object {
        private const val MIN_NICKNAME_LENGTH = 3
        private const val MAX_NICKNAME_LENGTH = 20
        private val ALLOWED_NICKNAME_REGEX = "^[a-zA-Z0-9]*$".toRegex() // Alphanumeric
    }

    /**
     * Validates the given nickname string for sign-up.
     *
     * @param nickname The nickname string to validate.
     * @return A [UserNameResult] indicating the outcome of the validation.
     */
    suspend operator fun invoke(nickname: String): UserNameResult {
        if (nickname.isBlank()) {
            return UserNameResult.Empty
        }
        if (nickname.length < MIN_NICKNAME_LENGTH) {
            return UserNameResult.TooShort(MIN_NICKNAME_LENGTH)
        }
        if (nickname.length > MAX_NICKNAME_LENGTH) {
            return UserNameResult.TooLong(MAX_NICKNAME_LENGTH)
        }
        if (!nickname.matches(ALLOWED_NICKNAME_REGEX)) {
            return UserNameResult.InvalidCharacters
        }

        try {
            when (val result = userRepository.observeByName(name = nickname).first()) {
                is CustomResult.Success -> {
                    // If a user is found, the nickname is already taken.
                    return UserNameResult.NicknameAlreadyExists
                }
                is CustomResult.Failure -> {
                    // If the specific error is 'NoSuchElementException', it means no user was found, so nickname IS available.
                    if (result.error is NoSuchElementException) {
                        return UserNameResult.Valid
                    } else {
                        // Other errors from the repository call.
                        return UserNameResult.Failure("Failed to check nickname availability: ${result.error.localizedMessage}")
                    }
                }
                is CustomResult.Loading -> {
                    // This state should ideally not be hit if .first() is used and the stream emits quickly.
                    return UserNameResult.Failure("Nickname availability check timed out or remained in loading state.")
                }
                is CustomResult.Initial -> {
                    return UserNameResult.Failure("Nickname availability check remained in initial state.")
                }
                is CustomResult.Progress -> {
                    return UserNameResult.Failure("Nickname availability check remained in progress state.")
                }
            }
        } catch (e: Exception) {
            // Catch exceptions from Flow collection (e.g., .first() on an empty flow if not handled by NoSuchElementException from source)
            // or other unexpected issues.
            if (e is NoSuchElementException) { // This might occur if the flow completes without emitting, though findByNameStream should emit Failure(NoSuchElementException)
                 return UserNameResult.Valid
            } else {
                 return UserNameResult.Failure("An unexpected error occurred during nickname validation: ${e.localizedMessage}")
            }
        }
    }
}
