package com.example.domain.model.vo.user

/**
 * Represents a user's profile image URL.
 * Allows nullable value (image may be absent) but validates format when present.
 */
@JvmInline
value class UserProfileImageUrl(val value: String?) {
    init {
        value?.let {
            require(it.length <= MAX_LENGTH) { "Profile image URL cannot exceed $MAX_LENGTH characters." }
            require(it.matches(URL_REGEX)) { "Invalid profile image URL format." }
        }
    }

    companion object {
        const val MAX_LENGTH = 500
        private val URL_REGEX = Regex("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$")
    }
}
