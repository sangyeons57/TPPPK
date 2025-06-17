package com.example.domain.model.vo.user

/**
 * Represents a user's name as a Value Object.
 * Ensures the name meets certain criteria (e.g., not empty, length limits).
 */
data class UserName(val value: String) {
    init {
        require(value.isNotBlank()) { "User name cannot be blank." }
        require(value.length <= MAX_LENGTH) { "User name cannot exceed $MAX_LENGTH characters." }
        // Add other validation rules if necessary (e.g., no special characters)
    }

    companion object {
        const val MAX_LENGTH = 50
    }
}
