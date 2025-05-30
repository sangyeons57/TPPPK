package com.example.domain.usecase.auth

import com.example.core_common.result.CustomResult
import com.example.domain.model.auth.NicknameValidationResult
import com.example.domain.repository.UserRepository
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
     * @return A [NicknameValidationResult] indicating the outcome of the validation.
     */
    suspend operator fun invoke(nickname: String): NicknameValidationResult {
        if (nickname.isBlank()) {
            return NicknameValidationResult.Empty
        }
        if (nickname.length < MIN_NICKNAME_LENGTH) {
            return NicknameValidationResult.TooShort(MIN_NICKNAME_LENGTH)
        }
        if (nickname.length > MAX_NICKNAME_LENGTH) {
            return NicknameValidationResult.TooLong(MAX_NICKNAME_LENGTH)
        }
        if (!nickname.matches(ALLOWED_NICKNAME_REGEX)) {
            return NicknameValidationResult.InvalidCharacters
        }

        return when (val result = userRepository.checkNicknameAvailability(nickname)) {
            is CustomResult.Success<Boolean> -> {
                if (result.data == true) { // True means available
                    NicknameValidationResult.Valid
                } else { // False means taken
                    NicknameValidationResult.NicknameAlreadyExists
                }
            }
            is CustomResult.Failure -> {
                NicknameValidationResult.Failure("Failed to check nickname availability.")
            }
            else -> {
                NicknameValidationResult.Failure("Unknown error occurred.")
            }
        }
    }
}
