package com.example.domain.model.auth

/**
 * Data class holding the parameters required for user registration.
 *
 * @property email The user's email address.
 * @property password The user's chosen password.
 * @property nickname The user's chosen nickname.
 */
data class UserRegistrationParams(
    val email: String,
    val password: String,
    val nickname: String
)
