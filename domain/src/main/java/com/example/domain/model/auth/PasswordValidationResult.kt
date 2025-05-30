package com.example.domain.model.auth

/**
 * Represents the result of a password validation attempt, specifically for sign-up or password changes.
 * It can indicate success (Valid) or various types of failures based on password complexity rules.
 */
sealed class PasswordValidationResult {
    /** Indicates the password is valid according to the defined rules. */
    object Valid : PasswordValidationResult()

    /** Indicates the password field was empty. */
    object Empty : PasswordValidationResult()

    /** Indicates the password is too short. */
    data class TooShort(val minimumLength: Int) : PasswordValidationResult()

    /** Indicates the password is too long. */
    data class TooLong(val maximumLength: Int) : PasswordValidationResult()

    /** Indicates the password lacks a required uppercase letter. */
    object MissingUppercase : PasswordValidationResult()

    /** Indicates the password lacks a required lowercase letter. */
    object MissingLowercase : PasswordValidationResult()

    /** Indicates the password lacks a required digit. */
    object MissingDigit : PasswordValidationResult()

    /** Indicates the password lacks a required special character. */
    object MissingSpecialCharacter : PasswordValidationResult()

    // Add other specific validation failures as needed.
}
