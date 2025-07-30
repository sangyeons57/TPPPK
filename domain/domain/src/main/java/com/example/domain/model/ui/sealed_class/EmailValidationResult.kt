package com.example.domain.model.ui.sealed_class

/**
 * Represents the result of an email validation attempt, specifically for sign-up.
 * It can indicate success (Valid) or various types of failures.
 * 이 모델은 UI 계층에서 이메일 검증 결과를 표시하는데 사용됩니다.
 */
sealed class EmailValidationResult {
    /** Indicates the email is valid for sign-up. */
    object Valid : EmailValidationResult()

    /** Indicates the email format is invalid. */
    object InvalidFormat : EmailValidationResult()

    /** Indicates the email is already registered. */
    object EmailAlreadyExists : EmailValidationResult()

    /** Indicates the email field was empty. */
    object Empty : EmailValidationResult()

    /**
     * Represents a generic failure during validation, potentially due to network issues
     * or other unexpected errors when checking for email existence.
     * @param message A descriptive error message.
     */
    data class Failure(val message: String?) : EmailValidationResult()
}
