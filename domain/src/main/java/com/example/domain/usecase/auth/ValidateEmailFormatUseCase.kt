package com.example.domain.usecase.auth

import android.util.Patterns
import javax.inject.Inject

/**
 * Use case to validate the format of an email address.
 */
class ValidateEmailFormatUseCase @Inject constructor() {

    /**
     * Validates the format of the given email string.
     *
     * @param email The email string to validate.
     * @return True if the email format is valid, false otherwise.
     */
    operator fun invoke(email: String): Boolean {
        if (email.isBlank()) {
            return false
        }
        // Using Android's built-in email pattern validator
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
