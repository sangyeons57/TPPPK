package com.example.domain.model.vo

/**
 * Represents an owner/user identifier. Wrapper around underlying String document id.
 * Shared across modules that refer to a user id (author, participant, etc.).
 */
@JvmInline
value class OwnerId(val value: String) {
    init {
//        require(value.isNotBlank()) { "OwnerId must not be blank." }
//        require(value.length <= MAX_LENGTH) { "OwnerId cannot exceed $MAX_LENGTH characters." }
    }

    companion object {
        const val MAX_LENGTH = 128
        
        fun from(userId: UserId): OwnerId {
            return OwnerId(userId.value)
        }
    }
}
