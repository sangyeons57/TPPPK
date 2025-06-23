package com.example.domain.usecase.auth.validation

import com.example.domain.model.ui.auth.EmailValidationResult
import javax.inject.Inject

/**
 * Use case to validate an email address for the sign-up process.
 * It checks for emptiness and format validity.
 * 
 * Email existence checking is now handled directly during signup for better security.
 *
 * @property validateEmailFormatUseCase Use case to validate the basic format of an email.
 */
class ValidateEmailForSignUpUseCase @Inject constructor(
    private val validateEmailFormatUseCase: ValidateEmailFormatUseCase
) {

    /**
     * Validates the given email string for sign-up.
     * Only performs basic format validation. Email existence is checked during actual signup.
     *
     * @param email The email string to validate.
     * @return An [EmailValidationResult] indicating the outcome of the validation.
     */
    operator fun invoke(email: String): EmailValidationResult {
        // Check if email is empty
        if (email.isBlank()) {
            return EmailValidationResult.Empty
        }

        // Check if email format is valid
        if (!validateEmailFormatUseCase(email)) {
            return EmailValidationResult.InvalidFormat
        }

        // If all checks pass, the email is considered valid
        // Note: Email existence check is now handled during the actual signup process
        // This is more secure and prevents email enumeration attacks
        return EmailValidationResult.Valid
    }
}
