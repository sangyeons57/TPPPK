package com.example.domain.usecase.auth

import com.example.domain.model.auth.PasswordValidationResult
import javax.inject.Inject

/**
 * Use case to validate a password, typically for sign-up or password change.
 * It checks for common password strength criteria.
 */
class ValidatePasswordForSignUpUseCase @Inject constructor() {

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        // Consider making MAX_PASSWORD_LENGTH if necessary, e.g., 64
    }

    /**
     * Validates the given password string.
     *
     * @param password The password string to validate.
     * @return A [PasswordValidationResult] indicating the outcome of the validation.
     */
    operator fun invoke(password: String): PasswordValidationResult {
        if (password.isBlank()) {
            return PasswordValidationResult.Empty
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return PasswordValidationResult.TooShort(MIN_PASSWORD_LENGTH)
        }
        // if (password.length > MAX_PASSWORD_LENGTH) { // Uncomment if max length is a rule
        //     return PasswordValidationResult.TooLong(MAX_PASSWORD_LENGTH)
        // }
        if (!password.any { it.isUpperCase() }) {
            return PasswordValidationResult.MissingUppercase
        }
        if (!password.any { it.isLowerCase() }) {
            return PasswordValidationResult.MissingLowercase
        }
        if (!password.any { it.isDigit() }) {
            return PasswordValidationResult.MissingDigit
        }
        // Regex for special characters: checks for at least one non-alphanumeric character.
        val specialCharPattern = Regex("[^A-Za-z0-9]")
        if (!password.contains(specialCharPattern)) {
            return PasswordValidationResult.MissingSpecialCharacter
        }

        return PasswordValidationResult.Valid
    }
}
